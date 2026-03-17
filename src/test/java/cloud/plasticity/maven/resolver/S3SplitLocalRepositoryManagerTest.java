package cloud.plasticity.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3SplitLocalRepositoryManagerTest {

    @TempDir
    Path tempDir;

    private S3SplitLocalRepositoryManager manager;
    private RepositorySystemSession session;
    private Path metadataDir;
    private Path artifactDir;

    @BeforeEach
    void setUp() {
        metadataDir = tempDir.resolve("metadata");
        artifactDir = tempDir.resolve("artifacts");
        
        session = mock(RepositorySystemSession.class);
        
        // Create a mock delegate
        LocalRepositoryManager delegate = mock(LocalRepositoryManager.class);
        when(delegate.getPathForLocalArtifact(any())).thenAnswer(invocation -> {
            Artifact artifact = invocation.getArgument(0);
            return artifact.getGroupId().replace('.', '/') + "/" +
                   artifact.getArtifactId() + "/" +
                   artifact.getVersion() + "/" +
                   artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
        });
        when(delegate.find(any(RepositorySystemSession.class), any(LocalArtifactRequest.class)))
            .thenAnswer(invocation -> new LocalArtifactResult(invocation.getArgument(1)));
        when(delegate.find(any(RepositorySystemSession.class), any(LocalMetadataRequest.class)))
            .thenAnswer(invocation -> new LocalMetadataResult(invocation.getArgument(1)));
        
        manager = new S3SplitLocalRepositoryManager(
            delegate,
            artifactDir,
            metadataDir
        );
    }

    @Test
    void testGetRepository() {
        assertNotNull(manager.getRepository());
        assertEquals(metadataDir.toFile(), manager.getRepository().getBasedir());
    }

    @Test
    void testGetPathForLocalArtifact() {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        String path = manager.getPathForLocalArtifact(artifact);
        
        assertTrue(path.contains("org/example/test/1.0"));
        assertTrue(path.endsWith(".jar"));
    }

    @Test
    void testFindArtifact_NotFound() {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        LocalArtifactRequest request = new LocalArtifactRequest(artifact, null, "");
        
        LocalArtifactResult result = manager.find(session, request);
        
        assertNotNull(result);
        assertFalse(result.isAvailable());
    }

    @Test
    void testFindArtifact_FoundInS3() throws Exception {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        
        // Create artifact file in S3 directory
        String path = manager.getPathForLocalArtifact(artifact);
        File artifactFile = artifactDir.resolve(path).toFile();
        artifactFile.getParentFile().mkdirs();
        Files.writeString(artifactFile.toPath(), "test content");
        
        LocalArtifactRequest request = new LocalArtifactRequest(artifact, null, "");
        LocalArtifactResult result = manager.find(session, request);
        
        assertNotNull(result);
        assertTrue(result.isAvailable());
        assertNotNull(result.getFile());
        assertEquals(artifactFile, result.getFile());
    }

    @Test
    void testAddArtifact() throws Exception {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        
        // Create source file in metadata directory
        String path = manager.getPathForLocalArtifact(artifact);
        Path sourceFile = metadataDir.resolve(path);
        sourceFile.getParent().toFile().mkdirs();
        Files.writeString(sourceFile, "test artifact content");
        
        LocalArtifactRegistration registration = new LocalArtifactRegistration(artifact);
        manager.add(session, registration);
        
        // Verify artifact was copied to S3 directory
        File targetFile = artifactDir.resolve(path).toFile();
        assertTrue(targetFile.exists());
        assertEquals("test artifact content", Files.readString(targetFile.toPath()));
    }

    @Test
    void testAddArtifact_NullFile() {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        LocalArtifactRegistration registration = new LocalArtifactRegistration(artifact);
        
        assertDoesNotThrow(() -> manager.add(session, registration));
    }

    @Test
    void testFindMetadata_NotFound() {
        Metadata metadata = mock(Metadata.class);
        when(metadata.getGroupId()).thenReturn("org.example");
        when(metadata.getArtifactId()).thenReturn("test");
        when(metadata.getVersion()).thenReturn("1.0");
        when(metadata.getType()).thenReturn("maven-metadata.xml");
        
        LocalMetadataRequest request = new LocalMetadataRequest(metadata, null, "");
        LocalMetadataResult result = manager.find(session, request);
        
        assertNotNull(result);
        // Metadata is delegated, so result depends on delegate behavior
    }

    @Test
    void testAddMetadata() {
        Metadata metadata = mock(Metadata.class);
        when(metadata.getGroupId()).thenReturn("org.example");
        when(metadata.getArtifactId()).thenReturn("test");
        when(metadata.getVersion()).thenReturn("1.0");
        when(metadata.getType()).thenReturn("maven-metadata.xml");
        
        LocalMetadataRegistration registration = new LocalMetadataRegistration(metadata);
        
        // Should not throw - metadata is delegated
        assertDoesNotThrow(() -> manager.add(session, registration));
    }

    @Test
    void testZeroCopyTransfer() throws Exception {
        Artifact artifact = new DefaultArtifact("org.example:large-file:1.0");
        
        // Create large source file (> 1MB to test loop)
        String path = manager.getPathForLocalArtifact(artifact);
        Path sourceFile = metadataDir.resolve(path);
        sourceFile.getParent().toFile().mkdirs();
        byte[] data = new byte[2 * 1024 * 1024]; // 2MB
        Files.write(sourceFile, data);
        
        LocalArtifactRegistration registration = new LocalArtifactRegistration(artifact);
        manager.add(session, registration);
        
        // Verify file was transferred
        File targetFile = artifactDir.resolve(path).toFile();
        assertTrue(targetFile.exists());
        assertEquals(data.length, targetFile.length());
    }

    @Test
    void testPathSeparation() throws Exception {
        Artifact artifact = new DefaultArtifact("org.example:test:1.0");
        
        // Create artifact in metadata directory
        String path = manager.getPathForLocalArtifact(artifact);
        Path sourceFile = metadataDir.resolve(path);
        sourceFile.getParent().toFile().mkdirs();
        Files.writeString(sourceFile, "content");
        
        LocalArtifactRegistration registration = new LocalArtifactRegistration(artifact);
        manager.add(session, registration);
        
        // Verify artifact is in artifactDir
        File artifactFile = artifactDir.resolve(path).toFile();
        assertTrue(artifactFile.exists());
        assertTrue(artifactFile.getAbsolutePath().startsWith(artifactDir.toString()));
        
        // Verify metadata would be in metadataDir
        File metadataBase = manager.getRepository().getBasedir();
        assertEquals(metadataDir.toFile(), metadataBase);
    }
}
