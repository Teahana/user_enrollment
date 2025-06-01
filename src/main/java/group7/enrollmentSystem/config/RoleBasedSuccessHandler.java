package group7.enrollmentSystem.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public RoleBasedSuccessHandler() {
        setDefaultTargetUrl("/home"); // fallback if no saved request and no roles matched
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws ServletException, IOException {

        // Check for original request
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);

        if (savedRequest != null) {
            // Let Spring handle redirecting to originally requested URL
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        // No saved request → redirect based on role priority
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = false;
        boolean isStudent = false;

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) isAdmin = true;
            if (role.equals("ROLE_STUDENT")) isStudent = true;
        }

        String redirectUrl;
        if (isAdmin) {
            redirectUrl = "/admin/dashboard";
        } else if (isStudent) {
            redirectUrl = "/home";
        } else {
            redirectUrl = getDefaultTargetUrl(); // fallback
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }


}

