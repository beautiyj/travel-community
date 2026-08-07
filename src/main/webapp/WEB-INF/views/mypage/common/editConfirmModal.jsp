<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<div class="cancel-modal" id="editConfirmModal" hidden>
    <div class="cancel-modal__dialog"
         role="alertdialog"
         aria-modal="true"
         aria-labelledby="edit-confirm-title"
         aria-describedby="edit-confirm-description">
        <div class="cancel-modal__header">
            <div>
                <h2 id="edit-confirm-title">회원정보 수정</h2>
                <p>입력한 내용으로 회원정보를 변경합니다.</p>
            </div>
            <button class="cancel-modal__close js-edit-confirm-close"
                    type="button" aria-label="닫기">×</button>
        </div>
        <div class="cancel-modal__body">
            <p id="edit-confirm-description"
               class="edit-confirm-modal__message">
                회원정보를 수정하시겠습니까?
            </p>
            <p class="edit-confirm-modal__notice">
                확인을 누르면 변경한 회원정보가 저장됩니다.
            </p>
        </div>
        <div class="cancel-modal__actions">
            <button class="cancel-modal__back js-edit-confirm-close"
                    type="button">돌아가기</button>
            <button class="cancel-modal__submit edit-confirm-modal__submit"
                    id="editConfirmSubmit" type="button">수정하기</button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/mypage/common/editConfirmModal.js"></script>
