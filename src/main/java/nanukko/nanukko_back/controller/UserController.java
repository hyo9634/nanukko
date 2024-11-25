package nanukko.nanukko_back.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nanukko.nanukko_back.domain.order.PaymentStatus;
import nanukko.nanukko_back.domain.product.ProductStatus;
import nanukko.nanukko_back.dto.page.PageResponseDTO;
import nanukko.nanukko_back.dto.review.ReviewInMyStoreDTO;
import nanukko.nanukko_back.dto.user.*;
import nanukko.nanukko_back.exception.ErrorResponse;
import nanukko.nanukko_back.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/my-store")
public class UserController {
    private final UserService userService;

    //사용자가 자신의 정보 조회
    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> getUserInfo(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId
    ) {
        UserInfoDTO userInfoDTO = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfoDTO);
    }

    //사용자가 자신의 정보 수정
    @PostMapping("/modify")
    public ResponseEntity<UserInfoDTO> modifyUserInfo(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestBody UserInfoDTO userInfoDTO
    ) {
        try {
            log.info("Received data: {}", userInfoDTO);  // 로깅 추가
            UserInfoDTO modifiedUser = userService.modifyUserInfo(userInfoDTO);
            return ResponseEntity.ok(modifiedUser);
        } catch (Exception e) {
            log.error("사용자 정보 수정 실패", e);  // 에러 로깅
            return ResponseEntity.badRequest().build();
        }
    }

    //사용자가 판매중, 판매완료 상품 조회
    @GetMapping("/sale-products")
    public ResponseEntity<PageResponseDTO<UserProductDTO>> getSellProducts(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "SELLING") ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponseDTO<UserProductDTO> products = userService.getSellProducts(userId, status, pageable);
        return ResponseEntity.ok(products);
    }

    //사용자가 판매중인 상품 수정
    @PostMapping("/sale-products/modify")
    public ResponseEntity<UserSetProductDTO> modifyProduct(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam Long productId,
            @RequestBody UserSetProductDTO productDTO
    ) {
        UserSetProductDTO response = userService.modifyProduct(userId, productId, productDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sale-products/remove")
    public ResponseEntity<?> removeProduct(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam Long productId
    ) {
        try {
            UserRemoveProductDTO response = userService.removeProduct(userId, productId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }


    //사용자가 구매중, 구매완료 상품 조회
    @GetMapping("/buy-products")
    public ResponseEntity<PageResponseDTO<UserOrderDTO>> getOrderProducts(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "ESCROW_HOLDING") PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponseDTO<UserOrderDTO> products = userService.getOrderProducts(userId, status, pageable);
        return ResponseEntity.ok(products);
    }

    //찜 목록 조회
    @GetMapping("/wishlist")
    public ResponseEntity<PageResponseDTO<UserWishlistDTO>> getWishlists(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponseDTO<UserWishlistDTO> wishlists = userService.getWishlists(userId, pageable);
        return ResponseEntity.ok(wishlists);
    }

    //찜 목록에서 제거
    @PostMapping("/wishlist/remove")
    public ResponseEntity<Void> removeWishlist(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam Long productId
    ) {
        userService.removeWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    //후기 조회
    @GetMapping("/reviews")
    public ResponseEntity<PageResponseDTO<ReviewInMyStoreDTO>> getReview(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponseDTO<ReviewInMyStoreDTO> reviews = userService.getReview(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    //탈퇴하기
    @PostMapping("/remove")
    public ResponseEntity<?> removeUser(
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
            @RequestParam String userId
    ) {
        try {
            UserRemoveDTO result = userService.removeUser(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
