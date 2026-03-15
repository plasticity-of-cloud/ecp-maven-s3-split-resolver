package cloud.plasticity.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Custom LocalRepositoryManager that splits artifact storage (S3) from metadata storage (local).
 *
 * Maven downloads artifacts to EmptyDir (supports rename for tmp files), then this manager
 * copies final artifacts to S3. Metadata/tracking files stay in EmptyDir.
 *
 * Flow:
 * 1. Maven downloads to EmptyDir (tmp + rename works)
 * 2. add() copies final artifact to S3 (sequential write, no rename)
 * 3. find() checks S3 for cached artifacts from previous builds
 */
public class S3SplitLocalRepositoryManager implements LocalRepositoryManager {

    private final LocalRepositoryManager delegate;
    private final Path artifactDir;
    private final Path metadataDir;
    private final LocalRepository localRepository;

    public S3SplitLocalRepositoryManager(LocalRepositoryManager delegate, Path artifactDir, Path metadataDir) {
        this.delegate = delegate;
        this.artifactDir = artifactDir;
        this.metadataDir = metadataDir;
        this.localRepository = new LocalRepository(metadataDir.toFile());
    }

    @Override
    public LocalRepository getRepository() {
        return localRepository;
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        return delegate.getPathForLocalArtifact(artifact);
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteArtifact(artifact, repository, context);
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        LocalArtifactResult result = delegate.find(session, request);
        if (!result.isAvailable()) {
            // Check S3 for artifacts cached from previous builds
            String relativePath = delegate.getPathForLocalArtifact(request.getArtifact());
            Path s3Path = artifactDir.resolve(relativePath);
            if (Files.exists(s3Path)) {
                result.setAvailable(true);
                result.setFile(s3Path.toFile());
            }
        }
        return result;
    }

    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        delegate.add(session, request);
        // Copy final artifact from EmptyDir to S3
        String relativePath = delegate.getPathForLocalArtifact(request.getArtifact());
        Path source = metadataDir.resolve(relativePath);
        Path dest = artifactDir.resolve(relativePath);
        if (Files.exists(source)) {
            try {
                Files.createDirectories(dest.getParent());
                Files.copy(source, dest);
            } catch (FileAlreadyExistsException e) {
                // Already on S3 from a previous build
            } catch (IOException e) {
                System.err.println("[S3SplitResolver] Failed to copy to S3: " + e.getMessage());
            }
        }
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        return delegate.find(session, request);
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        delegate.add(session, request);
    }
}
