/**
 * 서버가 선택된 상태의 신청 목록만 전달하므로, 그 결과의 표시 여부만 바꿔 화면 단 페이징을 제공한다.
 * 빈 목록에서는 컨트롤을 만들지 않으며, 한 페이지만 있어도 현재 페이지인 1을 표시한다.
 */
document.addEventListener('DOMContentLoaded', function () {
    const rowsContainer = document.querySelector('[data-business-application-rows]');
    const pagination = document.querySelector('[data-business-application-pagination]');

    if (!rowsContainer || !pagination) {
        return;
    }

    const rows = Array.from(rowsContainer.querySelectorAll('tr'));
    const pageSize = 10;
    const blockSize = 5;
    const pageCount = Math.ceil(rows.length / pageSize);
    let currentPage = 1;

    if (pageCount === 0) {
        return;
    }

    function createButton(label, page, className, ariaLabel) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'admin-btn admin-btn--outline admin-btn--sm admin-pagination__button ' + className;
        button.textContent = label;
        button.setAttribute('aria-label', ariaLabel);
        button.addEventListener('click', function () {
            // 버튼을 다시 그린 뒤에도 키보드 사용자가 현재 위치를 잃지 않도록 한다.
            renderPage(page, true);
        });
        return button;
    }

    function renderPage(page, shouldFocusCurrentPage) {
        // 서버 데이터나 URL은 바꾸지 않고, 이미 렌더링된 행의 hidden 상태만 현재 페이지에 맞춘다.
        currentPage = page;
        const firstRowIndex = (currentPage - 1) * pageSize;

        rows.forEach(function (row, index) {
            row.hidden = index < firstRowIndex || index >= firstRowIndex + pageSize;
        });

        pagination.replaceChildren();

        // 페이지 블록은 5개 단위이며, 마지막 블록은 실제 마지막 페이지까지만 표시한다.
        const startPage = Math.floor((currentPage - 1) / blockSize) * blockSize + 1;
        const endPage = Math.min(startPage + blockSize - 1, pageCount);

        if (currentPage > 1) {
            pagination.append(createButton('<', currentPage - 1, 'admin-pagination__button--direction', '이전 페이지'));
        }

        for (let pageNumber = startPage; pageNumber <= endPage; pageNumber += 1) {
            const button = createButton(String(pageNumber), pageNumber, 'admin-pagination__button--page', pageNumber + '페이지로 이동');

            if (pageNumber === currentPage) {
                button.classList.add('is-active');
                button.setAttribute('aria-current', 'page');
            }

            pagination.append(button);
        }

        if (currentPage < pageCount) {
            pagination.append(createButton('>', currentPage + 1, 'admin-pagination__button--direction', '다음 페이지'));
        }

        if (shouldFocusCurrentPage) {
            pagination.querySelector('[aria-current="page"]').focus();
        }
    }

    renderPage(currentPage, false);
});
