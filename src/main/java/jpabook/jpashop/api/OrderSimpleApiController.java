package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.OrderSimpleQueryDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * x To One (ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    // 엔티티를 직접 노출시키면 안된다
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        // all 안에 있는 Member는 proxy인 상태임. LAZY로 되어 있기 때문에
        // 저걸 그대로 JSON으로 변환하려고 하면 변환 라이브러리인 Jackson이 에러를 뱉음 (Member를 객체로 만들어야 하는데 MemberProxy 객체가 있으니까)
        // 그래서 아래처럼 Member를 조회해서 지연로딩을 일부러 시키는 것
        for (Order order : all) {
            order.getMember().getName(); // 프록시 강제 초기화
            order.getDelivery().getAddress(); // 프록시 강제 초기화
        }
        return all;
    }

    // 성능 이슈가 있다
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // N + 1 문제 발생
        // Order가 2개일 때 Order 한 번 쿼리 긁어오고 Member, Delivery 각각 2번씩...
        // 총 5개 돔..
        // 만약 10개를 출력하면 1 + 20 = 21번 쿼리 발생생
        return orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    // 페치조인이 사용되어 성능 이슈를 해결했지만
    // 엔티티를 그대로 가져와 사용하지 않는 컬럼 정보도 가져옴
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    // 성능최적화는 완전히 되었지만 구조 자체가 굳어졌기 때문에 확장성이 매우 제한적
    // v3와 v4를 섞어서 쓰도록 하자. 만약 필요한 데이터는 1-3개 컬럼인데 테이블에서 100개의 컬럼을 긁어오는 경우
    // v4를 쓰는 것을 고려해볼만 함
    // 성능테스트를 진행해보고 고객의 트래픽 등을 복합적으로 고려하고 전환 필요성이 있을 때 전환 요망
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderRepository.findOrderDtos();
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
}
