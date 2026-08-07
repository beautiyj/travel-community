(() => {
    const modal = document.getElementById("wishlistDeleteModal");
    const wishlistIdInput = document.getElementById("wishlistDeleteId");
    const placeLabel = document.getElementById("wishlistDeletePlace");
    const closeModal = () => {
        modal.hidden = true;
    };

    document.querySelectorAll(".js-wishlist-delete-open").forEach((button) => {
        button.addEventListener("click", () => {
            wishlistIdInput.value = button.dataset.wishlistId;
            placeLabel.textContent = "장소 #" + button.dataset.placeId;
            modal.hidden = false;
        });
    });

    document.querySelectorAll(".js-wishlist-delete-close").forEach((button) => {
        button.addEventListener("click", closeModal);
    });

    modal.addEventListener("click", (event) => {
        if (event.target === modal) closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) closeModal();
    });
})();
