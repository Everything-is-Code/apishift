package io.gateforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialViewMapperTest {

    @Test
    void threeScaleSourceView_omitsToken_andReportsConfigured() {
        var source = new ThreeScaleSource("lab", "Lab", "https://3scale.example.com", "secret-token", true);
        ThreeScaleSourceView view = ThreeScaleSourceView.from(source);

        assertTrue(view.credentialConfigured());
        assertTrue(source.accessToken().contains("secret"));
    }

    @Test
    void threeScaleSourceView_noneToken_isUnconfigured() {
        var view = ThreeScaleSourceView.from(
                new ThreeScaleSource("x", "X", "https://x", "none", true));
        assertFalse(view.credentialConfigured());
    }

    @Test
    void threeScaleSourceView_blankToken_isUnconfigured() {
        var view = ThreeScaleSourceView.from(
                new ThreeScaleSource("x", "X", "https://x", "", true));
        assertFalse(view.credentialConfigured());
    }

    @Test
    void targetClusterView_localCluster_isUnconfigured() {
        TargetClusterView view = TargetClusterView.from(TargetCluster.local());
        assertFalse(view.credentialConfigured());
    }

    @Test
    void targetClusterView_withToken_isConfigured() {
        var cluster = new TargetCluster("remote", "Remote", "https://api.example.com", "kube-token", "token", true, true);
        assertTrue(TargetClusterView.from(cluster).credentialConfigured());
    }
}
