<%@ include file="header.jsp" %>

<div class="card">
    <h2>Create Account</h2>

    <form action="/register" method="post">

        Name:
        <input type="text" name="name">

        Email:
        <input type="email" name="email">

        Password:
        <input type="password" name="password">

        <br><br>
        <button class="btn">Register</button>

    </form>
</div>