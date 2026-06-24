package io.apishift.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadProtectionAuthorizationFilterTest {

    @Test
    void isProtectedReadPath_migrationPlans() {
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/migration/plans"));
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/migration/plans/plan-1"));
    }

    @Test
    void isProtectedReadPath_policyMappingPublic() {
        assertFalse(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/migration/policy-mapping"));
    }

    @Test
    void isProtectedReadPath_hubAndAudit() {
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/hub/overview"));
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/audit/reports"));
    }

    @Test
    void isProtectedReadPath_productsAndTargets() {
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/threescale/products"));
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/cluster/targets"));
        assertTrue(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/cluster/targets/lab/validate"));
    }

    @Test
    void isProtectedReadPath_publicEndpoints() {
        assertFalse(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/cluster/features"));
        assertFalse(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/threescale/status"));
        assertFalse(ReadProtectionAuthorizationFilter.isProtectedReadPath("/api/chat/status"));
    }
}
