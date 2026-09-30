<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- One-shot messages. Read then cleared, so a refresh does not repeat them. --%>
<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash flash--success" role="status">
    <span class="flash__icon" aria-hidden="true">&#10003;</span>
    <span><c:out value="${sessionScope.flashSuccess}" /></span>
  </div>
  <c:remove var="flashSuccess" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash flash--error" role="alert">
    <span class="flash__icon" aria-hidden="true">!</span>
    <span><c:out value="${sessionScope.flashError}" /></span>
  </div>
  <c:remove var="flashError" scope="session" />
</c:if>
