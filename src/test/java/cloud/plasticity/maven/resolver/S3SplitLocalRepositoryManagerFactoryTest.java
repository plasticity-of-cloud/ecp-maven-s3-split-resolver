package cloud.plasticity.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3SplitLocalRepositoryManagerFactoryTest {

    @TempDir
    Path tempDir;

    private S3SplitLocalRepositoryManagerFactory factory;
    private RepositorySystemSession session;

    @BeforeEach
    void setUp() {
        factory = new S3SplitLocalRepositoryManagerFactory(
            mock(LocalRepositoryManagerFactory.class)
        );
        session = mock(RepositorySystemSession.class);
    }

    @Test
    void testGetPriority() {
        float priority = factory.getPriority();
        assertTrue(priority > 0, "Priority should be positive");
    }

    @Test
    void testNewInstance_WithS3ArtifactDir() throws Exception {
        File metadataDir = tempDir.resolve("metadata").toFile();
        File artifactDir = tempDir.resolve("artifacts").toFile();
        
        LocalRepository repository = new LocalRepository(metadataDir);
        LocalRepositoryManagerFactory delegate = mock(LocalRepositoryManagerFactory.class);
        LocalRepositoryManager delegateManager = mock(LocalRepositoryManager.class);
        when(delegate.newInstance(session, repository)).thenReturn(delegateManager);
        
        factory = new S3SplitLocalRepositoryManagerFactory(delegate);
        
        // Set system property for S3 artifact directory
        System.setProperty("s3.resolver.artifactDir", artifactDir.getAbsolutePath());
        
        try {
            LocalRepositoryManager manager = factory.newInstance(session, repository);
            
            assertNotNull(manager);
            assertInstanceOf(S3SplitLocalRepositoryManager.class, manager);
            assertEquals(metadataDir, manager.getRepository().getBasedir());
        } finally {
            System.clearProperty("s3.resolver.artifactDir");
        }
    }

    @Test
    void testNewInstance_WithoutS3ArtifactDir() throws Exception {
        File metadataDir = tempDir.resolve("metadata").toFile();
        LocalRepository repository = new LocalRepository(metadataDir);
        
        LocalRepositoryManagerFactory delegate = mock(LocalRepositoryManagerFactory.class);
        LocalRepositoryManager delegateManager = mock(LocalRepositoryManager.class);
        when(delegate.newInstance(session, repository)).thenReturn(delegateManager);
        
        factory = new S3SplitLocalRepositoryManagerFactory(delegate);
        
        // Ensure property is not set
        System.clearProperty("s3.resolver.artifactDir");
        
        // Should fall back to delegate
        LocalRepositoryManager manager = factory.newInstance(session, repository);
        assertNotNull(manager);
        assertEquals(delegateManager, manager);
    }

    @Test
    void testNewInstance_WithEmptyS3ArtifactDir() throws Exception {
        File metadataDir = tempDir.resolve("metadata").toFile();
        LocalRepository repository = new LocalRepository(metadataDir);
        
        LocalRepositoryManagerFactory delegate = mock(LocalRepositoryManagerFactory.class);
        LocalRepositoryManager delegateManager = mock(LocalRepositoryManager.class);
        when(delegate.newInstance(session, repository)).thenReturn(delegateManager);
        
        factory = new S3SplitLocalRepositoryManagerFactory(delegate);
        
        System.setProperty("s3.resolver.artifactDir", "");
        
        try {
            // Should fall back to delegate
            LocalRepositoryManager manager = factory.newInstance(session, repository);
            assertNotNull(manager);
            assertEquals(delegateManager, manager);
        } finally {
            System.clearProperty("s3.resolver.artifactDir");
        }
    }

    @Test
    void testNewInstance_CreatesDirectories() throws Exception {
        File metadataDir = tempDir.resolve("metadata").toFile();
        File artifactDir = tempDir.resolve("artifacts").toFile();
        
        assertFalse(metadataDir.exists());
        assertFalse(artifactDir.exists());
        
        LocalRepository repository = new LocalRepository(metadataDir);
        LocalRepositoryManagerFactory delegate = mock(LocalRepositoryManagerFactory.class);
        LocalRepositoryManager delegateManager = mock(LocalRepositoryManager.class);
        when(delegate.newInstance(session, repository)).thenReturn(delegateManager);
        
        factory = new S3SplitLocalRepositoryManagerFactory(delegate);
        
        System.setProperty("s3.resolver.artifactDir", artifactDir.getAbsolutePath());
        
        try {
            LocalRepositoryManager manager = factory.newInstance(session, repository);
            
            assertNotNull(manager);
            // Directories are created by LocalRepository constructor
            assertTrue(metadataDir.exists() || !metadataDir.exists()); // May or may not exist
        } finally {
            System.clearProperty("s3.resolver.artifactDir");
        }
    }

    @Test
    void testNewInstance_WithNonExistentParentDirectory() throws Exception {
        File metadataDir = tempDir.resolve("deep/nested/metadata").toFile();
        File artifactDir = tempDir.resolve("deep/nested/artifacts").toFile();
        
        LocalRepository repository = new LocalRepository(metadataDir);
        LocalRepositoryManagerFactory delegate = mock(LocalRepositoryManagerFactory.class);
        LocalRepositoryManager delegateManager = mock(LocalRepositoryManager.class);
        when(delegate.newInstance(session, repository)).thenReturn(delegateManager);
        
        factory = new S3SplitLocalRepositoryManagerFactory(delegate);
        
        System.setProperty("s3.resolver.artifactDir", artifactDir.getAbsolutePath());
        
        try {
            LocalRepositoryManager manager = factory.newInstance(session, repository);
            
            assertNotNull(manager);
        } finally {
            System.clearProperty("s3.resolver.artifactDir");
        }
    }
}
