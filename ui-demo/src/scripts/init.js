document.addEventListener('DOMContentLoaded', () => {
  if (window.lucide) {
    window.lucide.createIcons()
  }

  const toggle = document.getElementById('mobile-nav-toggle')
  const panel = document.getElementById('mobile-nav-panel')

  if (toggle && panel) {
    toggle.addEventListener('click', () => {
      panel.classList.toggle('is-open')
    })
  }
})
