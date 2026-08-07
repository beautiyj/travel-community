(() => {
    const modal = document.getElementById('cancelModal');
    const form = document.getElementById('cancelRequestForm');
    const reasonSelect = document.getElementById('cancelReason');
    const customReasonField = document.getElementById('cancelCustomReason');
    const customReasonText = document.getElementById('cancelReasonText');
    const requestError = document.getElementById('cancelRequestError');
    const submitButton = form.querySelector('.cancel-modal__submit');
    const close = () => { modal.hidden = true; };
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';
    document.querySelectorAll('.js-cancel-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelPlaceName').textContent = button.dataset.placeName;
        document.getElementById('cancelVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelHeadcount').textContent = button.dataset.headcount + '명';
        document.getElementById('cancelAmount').textContent = formatAmount(button.dataset.amount);
        reasonSelect.value = '';
        customReasonField.hidden = true;
        customReasonText.required = false;
        customReasonText.value = '';
        requestError.hidden = true;
        requestError.textContent = '';
        modal.hidden = false;

        const cancelRefundAmount = document.getElementById('cancelRefundAmount');
        cancelRefundAmount.textContent = '조회 중...';
        fetch(form.dataset.endpointPrefix + encodeURIComponent(button.dataset.reservationId) + '/refund-preview')
            .then((res) => res.json())
            .then((data) => { cancelRefundAmount.textContent = formatAmount(data.refundAmount); })
            .catch(() => { cancelRefundAmount.textContent = '-'; });
    }));
    document.querySelectorAll('.js-cancel-close').forEach((button) => button.addEventListener('click', close));
    reasonSelect.addEventListener('change', (event) => {
        const isCustom = event.target.value === 'CUSTOM';
        customReasonField.hidden = !isCustom;
        customReasonText.required = isCustom;
        if (isCustom) customReasonText.focus();
    });
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const reservationId = document.getElementById('cancelReservationId').value;
        const reason = reasonSelect.value === 'CUSTOM'
            ? customReasonText.value.trim()
            : reasonSelect.value;

        if (!reservationId || !reason) return;

        requestError.hidden = true;
        submitButton.disabled = true;

        try {
            const response = await fetch(
                form.dataset.endpointPrefix + encodeURIComponent(reservationId) + '/cancel-request',
                {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body: new URLSearchParams({reason: reason}).toString()
                }
            );

            if (!response.ok) throw new Error('cancel request failed');
            window.location.assign(form.dataset.successUrl);
        } catch (error) {
            requestError.textContent = '취소 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.';
            requestError.hidden = false;
            submitButton.disabled = false;
        }
    });
    modal.addEventListener('click', (event) => { if (event.target === modal) close(); });
})();

(() => {
    const modal = document.getElementById('cancelNowModal');
    const form = document.getElementById('cancelNowForm');
    const errorBox = document.getElementById('cancelNowError');
    const submitButton = form.querySelector('.cancel-modal__submit');
    const close = () => { modal.hidden = true; };
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';

    document.querySelectorAll('.js-cancel-now-open').forEach((button) => button.addEventListener('click', () => {
        document.getElementById('cancelNowReservationId').value = button.dataset.reservationId;
        document.getElementById('cancelNowPlaceName').textContent = button.dataset.placeName;
        document.getElementById('cancelNowVisitDate').textContent = button.dataset.visitDate;
        document.getElementById('cancelNowHeadcount').textContent = button.dataset.headcount + '명';
        document.getElementById('cancelNowAmount').textContent = formatAmount(button.dataset.amount);
        errorBox.hidden = true;
        errorBox.textContent = '';
        modal.hidden = false;
    }));

    document.querySelectorAll('.js-cancel-now-close').forEach((button) => button.addEventListener('click', close));

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const reservationId = document.getElementById('cancelNowReservationId').value;
        if (!reservationId) return;

        errorBox.hidden = true;
        submitButton.disabled = true;

        try {
            const response = await fetch(
                form.dataset.endpointPrefix + encodeURIComponent(reservationId) + '/cancel-request',
                { method: 'POST' }
            );
            if (!response.ok) throw new Error('cancel failed');
            window.location.assign(form.dataset.successUrl);
        } catch (error) {
            errorBox.textContent = '취소 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.';
            errorBox.hidden = false;
            submitButton.disabled = false;
        }
    });

    modal.addEventListener('click', (event) => { if (event.target === modal) close(); });
})();

(() => {
    const modal = document.getElementById('detailModal');
    const statusElement = document.getElementById('detailStatus');
    const processElement = document.getElementById('detailProcess');
    const cancelReasonBlock = document.getElementById('detailCancelReasonBlock');
    const cancelRejectReasonBlock = document.getElementById('detailCancelRejectReasonBlock');
    const rejectReasonBlock = document.getElementById('detailRejectReasonBlock');
    const noReason = document.getElementById('detailNoReason');

    const hasText = (value) => Boolean(value && value.trim());
    const formatAmount = (amount) => Number(amount || 0).toLocaleString('ko-KR') + '원';

    const getDetailState = ({status, cancelReason, cancelRejectReason, rejectReason}) => {
        const isCancelled = status === 'CANCELED' || status === 'CANCELLED';
        const hasCancelReason = hasText(cancelReason);
        const hasCancelRejectReason = hasText(cancelRejectReason);
        const hasRejectReason = hasText(rejectReason);

        if (isCancelled && hasCancelReason && !hasCancelRejectReason && !hasRejectReason) {
            return {label: '예약 취소', process: '예약 취소 처리 완료', type: 'cancelled'};
        }
        if (isCancelled && hasRejectReason && !hasCancelReason && !hasCancelRejectReason) {
            return {label: '예약 거절', process: '업체 예약 거절 처리 완료', type: 'rejected'};
        }
        if (status === 'CONFIRMED' && hasCancelReason && hasCancelRejectReason) {
            return {label: '예약 확정', process: '취소 요청 거절 · 예약 확정 유지', type: 'confirmed'};
        }

        const states = {
            PAID: {label: '결제완료', process: '업체 확인 대기', type: 'paid'},
            CANCEL_REQUESTED: {label: '취소요청', process: '업체의 취소 요청 확인 대기', type: 'requested'},
            CONFIRMED: {label: '예약 확정', process: '예약 확정', type: 'confirmed'},
            COMPLETED: {label: '이용 완료', process: '이용 완료', type: 'completed'},
            CANCELED: {label: '예약취소', process: '예약취소', type: 'cancelled'},
            CANCELLED: {label: '예약취소', process: '예약취소', type: 'cancelled'},
            EXPIRED: {label: '예약만료', process: '예약만료', type: 'expired'}
        };

        return states[status] || {label: '예약대기', process: '예약 대기', type: 'pending'};
    };

    document.querySelectorAll('.js-detail-open').forEach((button) => button.addEventListener('click', () => {
        const {dataset} = button;
        const state = getDetailState(dataset);

        document.getElementById('detailPlaceName').textContent = dataset.placeName || '장소 정보 없음';
        document.getElementById('detailPlace').textContent = dataset.placeName || '-';
        document.getElementById('detailVisitorName').textContent = dataset.visitorName || '-';
        document.getElementById('detailPhone').textContent = dataset.phone || '-';
        document.getElementById('detailVisitDate').textContent = dataset.visitDate || '-';
        document.getElementById('detailHeadcount').textContent = (dataset.headcount || '0') + '명';
        document.getElementById('detailAmount').textContent = formatAmount(dataset.amount);
        statusElement.textContent = state.label;
        statusElement.className = 'detail-modal__status detail-modal__status--' + state.type;
        processElement.textContent = state.process;

        const reasonBlocks = [
            {block: cancelReasonBlock, text: document.getElementById('detailCancelReason'), value: dataset.cancelReason},
            {block: cancelRejectReasonBlock, text: document.getElementById('detailCancelRejectReason'), value: dataset.cancelRejectReason},
            {block: rejectReasonBlock, text: document.getElementById('detailRejectReason'), value: dataset.rejectReason}
        ];
        let hasReason = false;
        reasonBlocks.forEach(({block, text, value}) => {
            const visible = hasText(value);
            block.hidden = !visible;
            if (visible) {
                text.textContent = value;
                hasReason = true;
            }
        });
        noReason.hidden = hasReason;
        modal.hidden = false;
    }));

    document.querySelectorAll('.js-detail-close').forEach((button) => button.addEventListener('click', () => { modal.hidden = true; }));
    modal.addEventListener('click', (event) => { if (event.target === modal) modal.hidden = true; });
})();
