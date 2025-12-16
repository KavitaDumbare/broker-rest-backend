package com.radianbroker.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.radianbroker.dto.Token;
import com.radianbroker.dto.UserBasicInfoDTO;
import com.radianbroker.dto.UserProfile;
import com.radianbroker.entity.Role;
import com.radianbroker.enums.Roles;
import com.radianbroker.payload.request.LaunchpadSessionRequest;
import com.radianbroker.payload.request.LoginRequest;
import com.radianbroker.payload.request.VerifyCodeRequest;
import com.radianbroker.payload.response.RefreshTokenResponse;
import com.radianbroker.security.UserDetailsImpl;
import com.radianbroker.service.UserService;
import com.radianbroker.service.impl.UserDetailsServiceImpl;
import com.radianbroker.utils.CookieUtil;
import com.radianbroker.utils.JwtUtils;
import com.radianbroker.utils.SecurityCipher;

import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	UserDetailsServiceImpl userDetailsService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	private CookieUtil cookieUtil;
	
	
	@Value("${radianapp.session.directlogin.enabled}")
	private boolean directloginEnabled;

	@Value("${radianapp.session.radian.url}")
	private String radianUrl;
	
	@GetMapping("/app-config")
	public ResponseEntity<?> getAppConfig() {
		try {
			  HashMap<String, Object> appConfig = new HashMap<>();
			  appConfig.put("directloginEnabled", directloginEnabled);
			  appConfig.put("radianUrl", radianUrl);
			return ResponseEntity.ok(appConfig);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getLocalizedMessage());
		}
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		try {
			String decodedPass = new String(Base64.getDecoder().decode(loginRequest.getPassword()));
			
			UserBasicInfoDTO userBasicInfoDTO = userDetailsService.loginUser(loginRequest.getEmail(),
					decodedPass);
			HttpHeaders httpHeaders = new HttpHeaders();
			List<Role> sessionRoles = userBasicInfoDTO.getRoles().stream()
			        .filter(role -> Roles.getSessionRoles().contains(role.getName()))
			        .collect(Collectors.toList());
			if (sessionRoles.size() == 1) {
				userBasicInfoDTO.setSessionRole(sessionRoles.get(0).getName());
				addSessionRoleCookie(httpHeaders, sessionRoles.get(0).getName());
			}
			return ResponseEntity.ok().headers(httpHeaders).body(userBasicInfoDTO);
		} catch (Exception e) {
			logger.error("Error found: {}", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/launchpad-signin")
	public ResponseEntity<?> authenticateLaunchpadUser(HttpServletRequest request,
			@RequestBody LaunchpadSessionRequest launchpadSessionRequest) {
		try {
			String userRemoteAddr = getRemoteAddr(request);
		
			UserDetails userDetails = userDetailsService.authenticateLaunchpadUser(launchpadSessionRequest.getSession());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
			UserProfile userProfile=userService.getUserProfile();
			Token accessToken = jwtUtils.generateAccessToken(userDetailsImpl.getEmail());
			Token refreshToken = jwtUtils.generateRefreshToken(userDetailsImpl.getEmail());

			HttpHeaders responseHeaders = new HttpHeaders();
			addAccessTokenCookie(responseHeaders, accessToken);
			addRefreshTokenCookie(responseHeaders, refreshToken);
			List<Role> sessionRoles = userProfile.getRoles().stream()
			        .filter(role -> Roles.getSessionRoles().contains(role.getName()))
			        .collect(Collectors.toList());
			if (sessionRoles.size() == 1) {
				userProfile.setSessionRole(sessionRoles.get(0).getName());
				addSessionRoleCookie(responseHeaders, sessionRoles.get(0).getName());
			}
			
			userDetailsService.createRedisUser(userDetailsImpl.getEmail(), accessToken.getTokenValue(),
					refreshToken.getTokenValue(), userRemoteAddr);
			return ResponseEntity.ok().headers(responseHeaders)
					.body(userService.getUserProfile());
			
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getLocalizedMessage());
		}
	}
	
	
	@PostMapping("/verify")
	public ResponseEntity<?> verifyCode(HttpServletRequest request,
			@Valid @RequestBody VerifyCodeRequest verifyCodeRequest) {
		try {
			String userRemoteAddr = getRemoteAddr(request);
			UserBasicInfoDTO userBasicInfoDTO = userDetailsService.verify(verifyCodeRequest.getEmail(),
					verifyCodeRequest.getCode());
			UserDetails userDetails = userDetailsService.loadUserByUsername(userBasicInfoDTO.getEmail());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, userDetails.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			Token accessToken = jwtUtils.generateAccessToken(userBasicInfoDTO.getEmail());
			Token refreshToken = jwtUtils.generateRefreshToken(userBasicInfoDTO.getEmail());

			HttpHeaders httpHeaders = new HttpHeaders();
			addAccessTokenCookie(httpHeaders, accessToken);
			addRefreshTokenCookie(httpHeaders, refreshToken);
			List<Role> sessionRoles = userBasicInfoDTO.getRoles().stream()
			        .filter(role -> Roles.getSessionRoles().contains(role.getName()))
			        .collect(Collectors.toList());
			if (sessionRoles.size() == 1) {
				userBasicInfoDTO.setSessionRole(sessionRoles.get(0).getName());
				addSessionRoleCookie(httpHeaders, sessionRoles.get(0).getName());
			}
			userDetailsService.createRedisUser(userBasicInfoDTO.getEmail(), accessToken.getTokenValue(),
					refreshToken.getTokenValue(), userRemoteAddr);
			return ResponseEntity.ok().headers(httpHeaders).body("Signin success!");
		} catch (Exception e) {
			logger.error("Error found: {}", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String getRemoteAddr(HttpServletRequest req) {
		if (!StringUtils.isEmpty(req.getHeader("X-Real-IP"))) {
			return req.getHeader("X-Real-IP");
		}
		if (!StringUtils.isEmpty(req.getHeader("X-Forwarded-For"))) {
			return req.getHeader("X-Forwarded-For");
		}
		return req.getRemoteAddr();
	}

	private void addAccessTokenCookie(HttpHeaders httpHeaders, Token token) {
		httpHeaders.add(HttpHeaders.SET_COOKIE,
				cookieUtil.createAccessTokenCookie(token.getTokenValue(), token.getDuration()).toString());
	}

	private void addRefreshTokenCookie(HttpHeaders httpHeaders, Token token) {
		httpHeaders.add(HttpHeaders.SET_COOKIE,
				cookieUtil.createRefreshTokenCookie(token.getTokenValue(), token.getDuration()).toString());
	}

	private void addSessionRoleCookie(HttpHeaders httpHeaders, String role) {
		long maxAge = -1;
		httpHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil.createSessionRoleCookie(role, maxAge).toString());
	}

	@PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> refreshToken(@CookieValue(name = "accessToken", required = false) String accessToken,
			@CookieValue(name = "refreshToken", required = false) String refreshToken) throws Exception {
		String decryptedAccessToken = SecurityCipher.decrypt(accessToken);
		String decryptedRefreshToken = SecurityCipher.decrypt(refreshToken);

		Token newAccessToken = userDetailsService.refreshToken(decryptedAccessToken, decryptedRefreshToken);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil
				.createAccessTokenCookie(newAccessToken.getTokenValue(), newAccessToken.getDuration()).toString());

		RefreshTokenResponse responseBody = new RefreshTokenResponse(RefreshTokenResponse.SuccessFailure.SUCCESS,
				"Auth successful. Tokens are created in cookie.");
		return ResponseEntity.ok().headers(responseHeaders).body(responseBody);
	}

	@PostMapping(value = "/signout", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> signOut(@CookieValue(name = "accessToken", required = false) String accessToken,
			@CookieValue(name = "refreshToken", required = false) String refreshToken) {

		if (accessToken != null && refreshToken != null) {
			String decryptedAccessToken = SecurityCipher.decrypt(accessToken);
			String decryptedRefreshToken = SecurityCipher.decrypt(refreshToken);

			userDetailsService.signOut(decryptedAccessToken, decryptedRefreshToken);
		}

		long maxAge = 0;
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(null, maxAge).toString());
		httpHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshTokenCookie(null, maxAge).toString());
		httpHeaders.add(HttpHeaders.SET_COOKIE, cookieUtil.createSessionRoleCookie(null, maxAge).toString());

		return ResponseEntity.ok().headers(httpHeaders).body("Signout success!");
	}

	@GetMapping("/send-2fa-otp")
	public ResponseEntity<?> send2faOtpEmail(@RequestParam(value = "userId", required = true) Long userId,
			@RequestParam(value = "email", required = true) String email) {
		try {
			return new ResponseEntity<>(userDetailsService.send2faOtpEmail(userId, email), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error found: {}", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	

}
