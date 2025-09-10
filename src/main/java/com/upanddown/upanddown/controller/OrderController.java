package com.upanddown.upanddown.controller;

import com.upanddown.upanddown.dto.OrderRequestDto;
import com.upanddown.upanddown.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주식 주문을 처리하는 API 엔드포인트.
     * lockType 파라미터에 따라 락킹 전략을 선택하여 주문을 실행합니다.
     *
     * @param request 주문 요청 데이터를 담은 DTO
     * @param lockType 사용할 락킹 전략 ("no-lock", "pessimistic", "optimistic")
     * @return 주문 처리 결과에 대한 응답
     */
    @PostMapping("/orders")
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequestDto request,
                                             @RequestParam(defaultValue = "no-lock") String lockType) {
        try {
            switch (lockType) {
                case "pessimistic":
                    orderService.placeOrder_PessimisticLock(request);
                    break;
                case "optimistic":
                    orderService.placeOrder_OptimisticLockWithRetry(request);
                    break;
                case "no-lock":
                default:
                    orderService.placeOrder_NoLock(request);
                    break;
            }
            return ResponseEntity.ok("주문이 성공적으로 처리되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("주문 실패: " + e.getMessage());
        }
    }

    /**
     * 판매 주문 등록 API
     * @param request 판매 주문 정보
     * @return 등록된 주문 ID
     */
    @PostMapping("/orders/sell")
    public ResponseEntity<Long> createSellOrder(@RequestBody OrderRequestDto request) {
        Long orderId = orderService.createSellOrder(request);
        return ResponseEntity.ok(orderId);
    }
}
