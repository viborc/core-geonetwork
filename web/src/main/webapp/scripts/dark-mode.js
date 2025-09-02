// Toggle dark mode
function toggleDarkMode(event) {
  if (event) {
    event.preventDefault();
  }
  
  var isDarkMode = localStorage.getItem('gn-dark-mode') === 'true';
  isDarkMode = !isDarkMode;
  
  localStorage.setItem('gn-dark-mode', isDarkMode);
  
  if (isDarkMode) {
    document.body.classList.add('gn-dark-theme');
    // Update button text and icon
    var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
    buttons.forEach(function(button) {
      var icon = button.querySelector('i');
      var span = button.querySelector('span');
      if (icon) {
        icon.classList.remove('fa-moon');
        icon.classList.add('fa-sun');
      }
      if (span) {
        span.textContent = 'Light mode';
      }
    });
  } else {
    document.body.classList.remove('gn-dark-theme');
    // Update button text and icon
    var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
    buttons.forEach(function(button) {
      var icon = button.querySelector('i');
      var span = button.querySelector('span');
      if (icon) {
        icon.classList.remove('fa-sun');
        icon.classList.add('fa-moon');
      }
      if (span) {
        span.textContent = 'Dark mode';
      }
    });
  }
}

// Initialize dark mode based on localStorage
(function() {
  var isDarkMode = localStorage.getItem('gn-dark-mode') === 'true';
  if (isDarkMode) {
    document.body.classList.add('gn-dark-theme');
    // Update button text and icon
    setTimeout(function() {
      var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
      buttons.forEach(function(button) {
        var icon = button.querySelector('i');
        var span = button.querySelector('span');
        if (icon) {
          icon.classList.remove('fa-moon');
          icon.classList.add('fa-sun');
        }
        if (span) {
          span.textContent = 'Light mode';
        }
      });
    }, 500);
  }
})();