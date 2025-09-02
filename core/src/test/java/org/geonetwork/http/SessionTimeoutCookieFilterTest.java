/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
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

package org.geonetwork.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import jeeves.server.UserSession;
import jeeves.server.sources.http.JeevesServlet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test class for SessionTimeoutCookieFilter.
 *
 * @author Test Generated
 */
public class SessionTimeoutCookieFilterTest {

    private SessionTimeoutCookieFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private HttpSession session;
    private UserSession userSession;
    private ServletContext servletContext;
    private SessionCookieConfig cookieConfig;

    @Before
    public void setUp() {
        filter = new SessionTimeoutCookieFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        userSession = mock(UserSession.class);
        servletContext = mock(ServletContext.class);
        cookieConfig = mock(SessionCookieConfig.class);

        request.setServletContext(servletContext);
        when(servletContext.getSessionCookieConfig()).thenReturn(cookieConfig);
        when(cookieConfig.isSecure()).thenReturn(false);
    }

    @Test
    public void testValidSessionWithAuthenticatedUser() throws IOException, ServletException {
        // Setup authenticated user
        String username = "testuser";
        when(userSession.getName()).thenReturn(username);
        when(session.getAttribute(JeevesServlet.USER_SESSION_ATTRIBUTE_KEY)).thenReturn(userSession);
        when(session.getMaxInactiveInterval()).thenReturn(1800); // 30 minutes
        
        request.setSession(session);
        request.setContextPath("/geonetwork");

        long beforeTime = System.currentTimeMillis();
        filter.doFilter(request, response, filterChain);
        long afterTime = System.currentTimeMillis();

        // Verify filter chain continued
        verify(filterChain).doFilter(request, response);

        // Verify cookies were added
        Cookie[] cookies = response.getCookies();
        assertNotNull("Cookies should be added", cookies);
        assertEquals("Should have 2 cookies", 2, cookies.length);

        Cookie serverTimeCookie = findCookieByName(cookies, "serverTime");
        Cookie sessionExpiryCookie = findCookieByName(cookies, "sessionExpiry");

        assertNotNull("serverTime cookie should exist", serverTimeCookie);
        assertNotNull("sessionExpiry cookie should exist", sessionExpiryCookie);

        // Verify cookie properties
        assertEquals("/geonetwork", serverTimeCookie.getPath());
        assertEquals("/geonetwork", sessionExpiryCookie.getPath());
        assertEquals(false, serverTimeCookie.getSecure());
        assertEquals(false, sessionExpiryCookie.getSecure());

        // Verify server time is within reasonable range
        long serverTime = Long.parseLong(serverTimeCookie.getValue());
        assertTrue("Server time should be current", serverTime >= beforeTime && serverTime <= afterTime);

        // Verify session expiry is set correctly (current time + session timeout)
        long sessionExpiry = Long.parseLong(sessionExpiryCookie.getValue());
        long expectedExpiry = serverTime + (1800 * 1000); // 30 minutes in milliseconds
        assertEquals("Session expiry should be server time + timeout", expectedExpiry, sessionExpiry);
    }

    @Test
    public void testValidSessionWithUnauthenticatedUser() throws IOException, ServletException {
        // Setup unauthenticated user (empty username)
        when(userSession.getName()).thenReturn("");
        when(session.getAttribute(JeevesServlet.USER_SESSION_ATTRIBUTE_KEY)).thenReturn(userSession);
        
        request.setSession(session);
        request.setContextPath("/geonetwork");

        long beforeTime = System.currentTimeMillis();
        filter.doFilter(request, response, filterChain);
        long afterTime = System.currentTimeMillis();

        // Verify filter chain continued
        verify(filterChain).doFilter(request, response);

        // Verify cookies were added
        Cookie[] cookies = response.getCookies();
        assertNotNull("Cookies should be added", cookies);
        assertEquals("Should have 2 cookies", 2, cookies.length);

        Cookie serverTimeCookie = findCookieByName(cookies, "serverTime");
        Cookie sessionExpiryCookie = findCookieByName(cookies, "sessionExpiry");

        assertNotNull("serverTime cookie should exist", serverTimeCookie);
        assertNotNull("sessionExpiry cookie should exist", sessionExpiryCookie);

        // Verify server time is within reasonable range
        long serverTime = Long.parseLong(serverTimeCookie.getValue());
        assertTrue("Server time should be current", serverTime >= beforeTime && serverTime <= afterTime);

        // For unauthenticated user, session expiry should equal server time
        long sessionExpiry = Long.parseLong(sessionExpiryCookie.getValue());
        assertEquals("Session expiry should equal server time for unauthenticated user", serverTime, sessionExpiry);
    }

    @Test
    public void testNoSessionNullSession() throws IOException, ServletException {
        // Setup request with no session
        request.setSession(null);

        filter.doFilter(request, response, filterChain);

        // Verify filter chain continued
        verify(filterChain).doFilter(request, response);

        // Verify no cookies were added
        Cookie[] cookies = response.getCookies();
        assertEquals("No cookies should be added when session is null", 0, cookies.length);
    }

    @Test
    public void testSessionWithEmptyContextPath() throws IOException, ServletException {
        // Setup authenticated user with empty context path
        String username = "testuser";
        when(userSession.getName()).thenReturn(username);
        when(session.getAttribute(JeevesServlet.USER_SESSION_ATTRIBUTE_KEY)).thenReturn(userSession);
        when(session.getMaxInactiveInterval()).thenReturn(1800);
        
        request.setSession(session);
        request.setContextPath(""); // Empty context path

        filter.doFilter(request, response, filterChain);

        // Verify filter chain continued
        verify(filterChain).doFilter(request, response);

        // Verify cookies were added with correct path
        Cookie[] cookies = response.getCookies();
        assertNotNull("Cookies should be added", cookies);
        assertEquals("Should have 2 cookies", 2, cookies.length);

        Cookie serverTimeCookie = findCookieByName(cookies, "serverTime");
        Cookie sessionExpiryCookie = findCookieByName(cookies, "sessionExpiry");

        assertNotNull("serverTime cookie should exist", serverTimeCookie);
        assertNotNull("sessionExpiry cookie should exist", sessionExpiryCookie);

        // Verify cookie path is "/" for empty context path
        assertEquals("Cookie path should be '/' for empty context path", "/", serverTimeCookie.getPath());
        assertEquals("Cookie path should be '/' for empty context path", "/", sessionExpiryCookie.getPath());
    }

    /**
     * Helper method to find a cookie by name in the cookie array.
     */
    private Cookie findCookieByName(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}