package cloud.plasticity.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating S3SplitLocalRepositoryManager instances.
 * 
 * Configuration via system properties:
 * - s3.resolver.artifactDir: Path to S3 mount for artifacts (default: ${maven.repo.local})
 * - maven.repo.local: Path to EmptyDir for metadata (standard Maven property)
 */
@Named("s3-split")
@Singleton
public class S3SplitLocalRepositoryManagerFactory implements LocalRepositoryManagerFactory {

    private static final String ARTIFACT_DIR_PROPERTY = "s3.resolver.artifactDir";
    private static final float PRIORITY = 100.0f; // Higher priority than default

    private final LocalRepositoryManagerFactory delegate;

    @Inject
    public S3SplitLocalRepositoryManagerFactory(@Named("enhanced") LocalRepositoryManagerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public LocalRepositoryManager newInstance(RepositorySystemSession session, LocalRepository repository)
            throws NoLocalRepositoryManagerException {
        
        String artifactDirProperty = System.getProperty(ARTIFACT_DIR_PROPERTY);
        
        if (artifactDirProperty == null || artifactDirProperty.isEmpty()) {
            // If not configured, fall back to delegate
            return delegate.newInstance(session, repository);
        }

        Path artifactDir = Paths.get(artifactDirProperty);
        Path metadataDir = repository.getBasedir().toPath();

        System.out.println("[S3SplitResolver] Artifacts: " + artifactDir);
        System.out.println("[S3SplitResolver] Metadata: " + metadataDir);

        LocalRepositoryManager delegateManager = delegate.newInstance(session, repository);
        return new S3SplitLocalRepositoryManager(delegateManager, artifactDir, metadataDir);
    }

    @Override
    public float getPriority() {
        return PRIORITY;
    }
}
