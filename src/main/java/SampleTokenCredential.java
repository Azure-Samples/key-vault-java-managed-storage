import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper class that supplies and caches the token for Azure authentication.
 */
public class SampleTokenCredential implements TokenCredential {
    private String authorization;
    private AccessToken mainAccessToken;
    private AccessToken subAccessToken;
    private String authClientId;

    public SampleTokenCredential(String tenantId, String authClientId) {
        authorization = "https://login.windows.net/" + tenantId;
        this.authClientId = authClientId;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext resource) {
        try {
            if (mainAccessToken == null && !"https://vault.azure.net".equals(ScopeUtil.scopesToResource(resource.getScopes()))) {
                // We only cache the auth token here, but for a longer running process the refresh token should also be cached.
                String mgmtToken = KeyVaultSampleBase.getAccessToken(authorization, ScopeUtil.scopesToResource(resource.getScopes()), authClientId);
                this.mainAccessToken = new AccessToken(mgmtToken, OffsetDateTime.now().plusDays(1));
            } else if (subAccessToken == null && "https://vault.azure.net".equals(ScopeUtil.scopesToResource(resource.getScopes()))  ) {
                String mgmtToken = KeyVaultSampleBase.getAccessToken(authorization, ScopeUtil.scopesToResource(resource.getScopes()), authClientId);
                this.subAccessToken = new AccessToken(mgmtToken, OffsetDateTime.now().plusDays(1));
                return Mono.just(subAccessToken);
            } if ( "https://vault.azure.net".equals(ScopeUtil.scopesToResource(resource.getScopes())) ) {
                return Mono.just(subAccessToken);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Mono.just(this.mainAccessToken);
    }
}
