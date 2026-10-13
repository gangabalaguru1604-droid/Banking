<%@ include file="header.jsp" %>

<div class="card">
    <h2>Apply Loan</h2>

    <form action="/applyLoan" method="post">

        Amount:
        <input type="number" name="amount" required>

        Interest (%):
        <input type="number" name="interest" required>

        Months:
        <input type="number" name="months" required>

        <br><br>
        <button class="btn">Apply</button>

    </form>
</div>