(function () {
    var accessLink = document.querySelector('[data-business-rejected-access]');
    var modal = document.getElementById('businessRejectedModal');
    if (!accessLink || !modal) return;

    var dialog = modal.querySelector('.modal');
    var cancelButton = modal.querySelector('.modal-btn-cancel');
    if (cancelButton) cancelButton.remove();

    var closeButton = document.createElement('button');
    closeButton.type = 'button';
    closeButton.className = 'business-rejected-modal__close';
    closeButton.setAttribute('aria-label', '닫기');
    closeButton.textContent = '×';
    dialog.insertBefore(closeButton, dialog.firstChild);

    var closeModal = function () {
        modal.classList.remove('is-open');
        document.body.classList.remove('modal-open');
    };

    accessLink.addEventListener('click', function (event) {
        event.preventDefault();
        modal.classList.add('is-open');
        document.body.classList.add('modal-open');
        closeButton.focus();
    });

    closeButton.addEventListener('click', closeModal);

    var form = modal.querySelector('form');
    if (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            closeModal();
        });
    }

    modal.addEventListener('click', function (event) {
        if (event.target === modal) closeModal();
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && modal.classList.contains('is-open')) {
            closeModal();
        }
    });
}());
