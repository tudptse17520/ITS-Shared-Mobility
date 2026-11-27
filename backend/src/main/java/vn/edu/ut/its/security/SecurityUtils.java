package vn.edu.ut.its.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.ut.its.constant.Role;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.exception.AuthenticationException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.UserRepository;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> getAuthenticatedUser(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            return userRepository.findById(u.getId()).or(() -> Optional.of(u));
        }
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername());
        }
        return Optional.empty();
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Resolves target user while enforcing ownership when a Spring Security principal is active.
     */
    public static User resolveAuthorizedUser(Long explicitUserId, UserRepository userRepository) {
        Optional<User> authUserOpt = getAuthenticatedUser(userRepository);
        if (authUserOpt.isPresent()) {
            User authUser = authUserOpt.get();
            if (explicitUserId != null && !explicitUserId.equals(authUser.getId())) {
                if (!isAdmin(authUser)) {
                    throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác thay cho tài khoản khác.");
                }
                return userRepository.findById(explicitUserId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy tài khoản người dùng ID: " + explicitUserId));
            }
            return authUser;
        }

        if (explicitUserId != null) {
            return userRepository.findById(explicitUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy tài khoản người dùng ID: " + explicitUserId));
        }

        throw new AuthenticationException("Vui lòng đăng nhập để thực hiện chức năng này.");
    }

    public static void assertAdminOrOwner(Long ownerUserId, UserRepository userRepository, String message) {
        Optional<User> authUserOpt = getAuthenticatedUser(userRepository);
        if (authUserOpt.isEmpty()) {
            return;
        }
        User currentUser = authUserOpt.get();
        if (isAdmin(currentUser)) {
            return;
        }
        if (ownerUserId == null || !ownerUserId.equals(currentUser.getId())) {
            throw new AccessDeniedException(message);
        }
    }
}
