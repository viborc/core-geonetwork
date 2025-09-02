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

(function () {
  "use strict";

  // Initialize dark mode based on localStorage
  function initDarkMode() {
    var isDarkMode = localStorage.getItem("gn-dark-mode") === "true";
    if (isDarkMode) {
      document.body.classList.add("gn-dark-theme");
    }

    // Update toggle button state
    var toggleButtons = document.querySelectorAll(".gn-theme-toggle");
    toggleButtons.forEach(function (button) {
      updateToggleButton(button, isDarkMode);
    });
  }

  // Update the toggle button appearance
  function updateToggleButton(button, isDarkMode) {
    var icon = button.querySelector("i");
    var darkModeText = button.querySelector(".dark-mode-text");
    var lightModeText = button.querySelector(".light-mode-text");

    if (icon) {
      if (isDarkMode) {
        icon.classList.remove("fa-moon");
        icon.classList.add("fa-sun");
      } else {
        icon.classList.remove("fa-sun");
        icon.classList.add("fa-moon");
      }
    }

    if (darkModeText && lightModeText) {
      darkModeText.style.display = isDarkMode ? "none" : "inline";
      lightModeText.style.display = isDarkMode ? "inline" : "none";
    }
  }

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
    } else {
      document.body.classList.remove("gn-dark-theme");
    }

    // Update all toggle buttons
    var toggleButtons = document.querySelectorAll(".gn-theme-toggle");
    toggleButtons.forEach(function (button) {
      updateToggleButton(button, isDarkMode);
    });
  }

  // Add event listeners to toggle buttons when DOM is loaded
  document.addEventListener("DOMContentLoaded", function () {
    initDarkMode();

    var toggleButtons = document.querySelectorAll(".gn-theme-toggle");
    toggleButtons.forEach(function (button) {
      button.addEventListener("click", toggleDarkMode);
    });
  });

  // Expose the toggle function globally
  window.gnToggleDarkMode = toggleDarkMode;
})();
