/*
 * Copyright (C) 2001-2024 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

// Toggle dark mode
function toggleDarkMode(event) {
  if (event) {
    event.preventDefault();
  }

  var isDarkMode = localStorage.getItem("gn-dark-mode") === "true";
  isDarkMode = !isDarkMode;

  localStorage.setItem("gn-dark-mode", isDarkMode);

  if (isDarkMode) {
    document.body.classList.add("gn-dark-theme");
    // Update button text and icon
    var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
    buttons.forEach(function (button) {
      var icon = button.querySelector("i");
      var span = button.querySelector("span");
      if (icon) {
        icon.classList.remove("fa-moon");
        icon.classList.add("fa-sun");
      }
      if (span) {
        span.textContent = "Light mode";
      }
    });
  } else {
    document.body.classList.remove("gn-dark-theme");
    // Update button text and icon
    var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
    buttons.forEach(function (button) {
      var icon = button.querySelector("i");
      var span = button.querySelector("span");
      if (icon) {
        icon.classList.remove("fa-sun");
        icon.classList.add("fa-moon");
      }
      if (span) {
        span.textContent = "Dark mode";
      }
    });
  }
}

// Initialize dark mode based on localStorage
(function () {
  var isDarkMode = localStorage.getItem("gn-dark-mode") === "true";
  if (isDarkMode) {
    document.body.classList.add("gn-dark-theme");
    // Update button text and icon
    setTimeout(function () {
      var buttons = document.querySelectorAll('a[onclick="toggleDarkMode(event)"]');
      buttons.forEach(function (button) {
        var icon = button.querySelector("i");
        var span = button.querySelector("span");
        if (icon) {
          icon.classList.remove("fa-moon");
          icon.classList.add("fa-sun");
        }
        if (span) {
          span.textContent = "Light mode";
        }
      });
    }, 500);
  }
})();
