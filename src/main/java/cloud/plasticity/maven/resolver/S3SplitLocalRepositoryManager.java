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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom LocalRepositoryManager that splits artifact storage (S3) from metadata storage (local).
 * 
 * Artifacts (JARs, POMs, checksums) are written to artifactDir (S3 mount, sequential writes only).
 * Metadata and tracking files are written to metadataDir (EmptyDir, supports random I/O).
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
        String relativePath = delegate.getPathForLocalArtifact(artifact);
        return metadataDir.relativize(artifactDir.resolve(relativePath)).toString();
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        String relativePath = delegate.getPathForRemoteArtifact(artifact, repository, context);
        return metadataDir.relativize(artifactDir.resolve(relativePath)).toString();
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        // Metadata stays in EmptyDir (delegate handles it)
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        // Metadata stays in EmptyDir (delegate handles it)
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        // Delegate to the underlying LRM for artifact lookup
        return delegate.find(session, request);
    }

    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        // Delegate handles registration (tracking files in EmptyDir)
        delegate.add(session, request);
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        // Delegate handles metadata lookup
        return delegate.find(session, request);
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        // Delegate handles metadata registration
        delegate.add(session, request);
    }
}
