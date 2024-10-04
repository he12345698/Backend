package com.example.HealthcareManager.Controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.HealthcareManager.Model.User;
import com.example.HealthcareManager.Repository.AccountRepository;
import com.example.HealthcareManager.Security.JwtService;
import com.example.HealthcareManager.Service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        return accountService.registerUser(user);
    }


    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> tokenData) {
        String idToken = tokenData.get("idToken");
        try {
            Optional<User> user = accountService.verifyGoogleToken(idToken);
            if (user.isPresent()) {
                User userInfo = user.get();
                System.out.println("ResponseEntity to app... ID：" + userInfo.getId() + " Username： " + userInfo.getUsername() + " Email： " + userInfo.getEmail());
                return ResponseEntity.ok(new User(userInfo.getId(), userInfo.getUsername(), userInfo.getEmail()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Google token or user not found.");
            }
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }
    
    @PostMapping("/facebook-login")
    public ResponseEntity<?> facebookLogin(@RequestBody String accessToken) {
    	System.out.println("accessToken at fblogin is" + accessToken);
    	try {
            Optional<User> user = accountService.verifyFacebookToken(accessToken);
            if (user.isPresent()) {
                User userInfo = user.get();
                System.out.println("ResponseEntity to app... ID：" + userInfo.getId() + " Username： " + userInfo.getUsername() + " Email： " + userInfo.getEmail());
                return ResponseEntity.ok(new User(userInfo.getId(), userInfo.getUsername(), userInfo.getEmail()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Google token or user not found.");
            }
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
        
    }
    
    @PostMapping("/line-callback")
    public ResponseEntity<User> lineCallback(@RequestBody Map<String, String> requestBody) {
        String code = requestBody.get("code");  // 从请求体中获取 "code"

        // 使用认证码交换访问令牌
        Optional<User> userInfoResponse = accountService.fetchUserInfoWithAccessToken(code);
        if (userInfoResponse.isPresent()) {
        	User user = userInfoResponse.get();
            User userResponse = new User(user.getId(), user.getUsername(), user.getImagelink());
            return ResponseEntity.ok(userResponse);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User user) {
    	System.out.println("user is " + user);
        return accountService.login(user);
    }

    @PostMapping("/validate-token")
public ResponseEntity<Map<String, String>> getProtectedData(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "無效的身份驗證");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }
    
    

    // @PostMapping("/test")
    // public String test() {
    //     return "test";
    // }

    String userId;
    if (authentication.getPrincipal() instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        userId = userDetails.getUsername(); // 获取用户 ID
        System.out.println(userId);
    } else if (authentication.getPrincipal() instanceof String) {
        userId = (String) authentication.getPrincipal(); // 直接获取用户名
    } else {
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "無法識別身份驗證對象");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
    }

    Optional<User> optionalUser = accountRepository.findById(userId); 
    if (optionalUser.isPresent()) {
        User user = optionalUser.get();
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("username", user.getUsername());
        responseBody.put("id", user.getId());
        responseBody.put("userImage", user.getImagelink());
        responseBody.put("email", user.getEmail());
        responseBody.put("password", user.getPassword());
        responseBody.put("gender", user.getGender());
        responseBody.put("height", user.getHeight().toString());
        responseBody.put("weight", user.getWeight().toString());
        responseBody.put("dateOfBirth", user.getDateOfBirth().toString());
        return ResponseEntity.ok(responseBody);
    } else {
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "伺服器錯誤：用户不存在");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
    }
}

}
