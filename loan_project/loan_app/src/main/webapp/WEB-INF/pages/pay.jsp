<%@ include file="header.jsp" %>

<div class="card">
    <h2>Pay EMI</h2>

    <form action="/pay" method="post">

        <!-- Hidden Loan ID -->
        <input type="hidden" name="loanId" value="${loan.loanId}">

        Amount:
        <input type="number" name="amount">

        <br><br>
        <button class="btn">Pay</button>

    </form>
</div>