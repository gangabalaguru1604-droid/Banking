<%@ include file="header.jsp" %>

<div class="card">
    <h2>Login</h2>

    <form action="/login" method="post">

        Email:
        <input type="text" name="email">

        Password:
        <input type="password" name="password">

        <br><br>
        <button class="btn">Login</button>

    </form>
</div>