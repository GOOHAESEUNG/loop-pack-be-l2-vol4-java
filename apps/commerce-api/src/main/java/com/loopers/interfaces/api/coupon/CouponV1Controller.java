package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponApplicationService;
import com.loopers.application.coupon.CouponIssueRequestInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponV1Controller {

    private final CouponApplicationService couponApplicationService;

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> issueCoupon(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @PathVariable Long couponId
    ) {
        couponApplicationService.issueCoupon(loginId, couponId);
        return ApiResponse.success(null);
    }

    /**
     * 선착순 쿠폰 발급 요청 (비동기). 발급은 Consumer가 수행하고 requestId로 결과를 조회한다.
     */
    @PostMapping("/{couponId}/issue-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CouponV1Dto.IssueRequestResponse> requestIssue(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @PathVariable Long couponId
    ) {
        String requestId = couponApplicationService.requestCouponIssue(loginId, couponId);
        return ApiResponse.success(new CouponV1Dto.IssueRequestResponse(requestId));
    }

    @GetMapping("/issue-requests/{requestId}")
    public ApiResponse<CouponV1Dto.IssueRequestStatusResponse> getIssueRequest(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @PathVariable String requestId
    ) {
        CouponIssueRequestInfo info = couponApplicationService.getIssueRequest(loginId, requestId);
        return ApiResponse.success(CouponV1Dto.IssueRequestStatusResponse.from(info));
    }
}
