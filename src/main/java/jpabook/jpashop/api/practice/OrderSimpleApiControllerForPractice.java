package jpabook.jpashop.api.practice;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiControllerForPractice {

    private final OrderRepository orderRepository;

    @GetMapping("/practice/api/v1/simple-orders")
    public Result<List<Order>> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        return new Result<>(all.size(), all);
    }

//    order 요청이 들어온다 -> order 조회 내보냄 (연관관계는 프록시 처리 why? tetchType이 LAZY이기 때문)
//
//    뷰단에서 보니 order 내 member와 delivery가 필요하다는 것을 발견.
//
//    member 요청 -> member 조회 내보냄
//    delivery 요청 -> delivery 조회 내보냄
//
//    order 요청 수마다 이 작업 반복
//
//    만약 order 요청이 10개라면 -> 쿼리는 1 + 2*10 -> 21개가 나감
    @GetMapping("/practice/api/v2/simple-orders")
    public Result ordersV2() {
        List<SimpleOrderDto> simpleOrderDtoList = orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
        return new Result(simpleOrderDtoList.size(), simpleOrderDtoList);
    }

    @GetMapping("/practice/api/v3/simple-orders")
    public Result ordersV3() {
        List<SimpleOrderDto> simpleOrderDtoList = orderRepository.findAllWithMemberDelivery().stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());

        return new Result(simpleOrderDtoList.size(), simpleOrderDtoList);
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }
}
