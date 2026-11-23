package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.ut.its.constant.Role;
import vn.edu.ut.its.constant.UserStatus;
import vn.edu.ut.its.dto.request.LoginRequest;
import vn.edu.ut.its.dto.request.RegisterRequest;
import vn.edu.ut.its.dto.response.LoginResponse;
import vn.edu.ut.its.dto.response.UserResponse;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.exception.AuthenticationException;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.service.AuthService;
import vn.edu.ut.its.util.JwtUtil;

/**
 * Hiện thực hóa nghiệp vụ xác thực người dùng.
 * Flow: Tìm user → Kiểm tra trạng thái ACTIVE → Verify password → Sinh JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Tìm User bằng username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));

        // 2. Kiểm tra trạng thái tài khoản
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Tài khoản đã bị vô hiệu hóa.");
        }

        // 3. Xác thực mật khẩu (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Mật khẩu không chính xác.");
        }

        // 4. Sinh JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        // 5. Trả về kết quả đăng nhập
        return LoginResponse.builder()
                .token(token)
                .message("Đăng nhập thành công.")
                .role(user.getRole())
                .build();
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String phone = request.getPhoneNumber().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Tên đăng nhập '" + username + "' đã tồn tại.");
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new ConflictException("Số điện thoại '" + phone + "' đã được đăng ký.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phoneNumber(phone)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getUsername(), saved.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .message("Đăng ký tài khoản thành công.")
                .role(saved.getRole())
                .build();
    }

    @Override
    public UserResponse getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new AuthenticationException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        User currentUser = (User) authentication.getPrincipal();
        String username = currentUser.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    @Override
    public void changePassword(vn.edu.ut.its.dto.request.ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new AuthenticationException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        User currentUser = (User) authentication.getPrincipal();
        String username = currentUser.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new vn.edu.ut.its.exception.BusinessRuleViolationException("Mật khẩu cũ không chính xác.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new vn.edu.ut.its.exception.BusinessRuleViolationException("Xác nhận mật khẩu không khớp.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
