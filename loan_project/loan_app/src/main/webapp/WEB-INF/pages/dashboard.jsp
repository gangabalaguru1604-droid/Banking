<%@ include file="header.jsp" %>

<h2 style="text-align:center;">Welcome ${sessionScope.user.name}</h2>

<div style="display:flex; justify-content:center; margin-top:40px;">

<c:choose>

<c:when test="${loan != null}">

    <div class="card" style="margin:20px;">
        <h3>Loan Amount</h3>
        <p>${loan.amount}</p>
    </div>

    <div class="card" style="margin:20px;">
        <h3>EMI</h3>
        <p>${loan.emi}</p>
    </div>

    <div class="card" style="margin:20px;">
        <h3>Remaining</h3>
        <p>${loan.remaining}</p>
    </div>

</c:when>

<c:otherwise>
    <h3>No Loan Applied Yet</h3>
</c:otherwise>

</c:choose>

</div>

<div style="text-align:center; margin-top:20px;">

    <a href="/applyLoan">
        <button class="btn" style="width:200px;">Apply Loan</button>
    </a>

    <c:if test="${loan != null}">
        <a href="/payPage">
            <button class="btn" style="width:200px;">Pay EMI</button>
        </a>
    </c:if>

</div>