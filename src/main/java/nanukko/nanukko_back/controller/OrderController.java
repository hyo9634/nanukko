package nanukko.nanukko_back.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nanukko.nanukko_back.dto.order.OrderConfirmDTO;
import nanukko.nanukko_back.dto.order.OrderPageDTO;
import nanukko.nanukko_back.dto.order.OrderResponseDTO;
import nanukko.nanukko_back.exception.ErrorResponse;
import nanukko.nanukko_back.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/payments")
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/page/{productId}")
    public ResponseEntity<OrderPageDTO> getOrderPage(
            @PathVariable Long productId
            //@AuthenticationPrincipal UserDetails userDetails  // 현재 로그인한 사용자(시큐리티)
    ) {
        OrderPageDTO dto = orderService.getOrder(productId, "buyer1"); //테스트를 위해 일단 사용자명을 임시로 정의

        return ResponseEntity.ok(dto);
        // return ResponseEntity.ok(orderService.getOrderPage(productId, userDetails.getUsername())); //시큐리티로 사용자 가져와서 반환하기
    }

    //결제 상세정보 조회
    @GetMapping("/{orderId}/detail")
    public OrderResponseDTO getPaymentDetails(@PathVariable String orderId) {
        return orderService.getPaymentDetail(orderId);
    }

    //결제 승인 -> 결제하고 나서 승인 처리 -> IN_PROGRESS 삳태를 DONE 으로 만들어주기
    @PostMapping("/confirm")
    public ResponseEntity<OrderResponseDTO> confirmPayment(@RequestBody OrderConfirmDTO request) {
        return ResponseEntity.ok(orderService.confirmPayment(request));
    }

    //구매확정
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponseDTO> confirmPurchase(@PathVariable String orderId) {
        OrderResponseDTO response = orderService.confirmPurchase(orderId);
        return ResponseEntity.ok(response);
    }

    //에스크로 상태 중에 결제 취소하기
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) {
        try {
            OrderResponseDTO response = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
