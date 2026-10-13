<%@ page contentType="text/html;charset=UTF-8" %>

<style>
body {
    margin: 0;
    font-family: 'Segoe UI', sans-serif;
    background-color: #fdf6ec;
}

/* NAVBAR */
.navbar {
    display: flex;
    justify-content: space-between;
    padding: 15px 40px;
    background: white;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.logo {
    font-size: 22px;
    font-weight: bold;
}

.nav a {
    margin-left: 20px;
    text-decoration: none;
    color: #333;
}

.nav a:hover {
    color: #e67e22;
}

/* CARD */
.card {
    width: 350px;
    margin: 60px auto;
    padding: 25px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 5px 20px rgba(0,0,0,0.1);
    animation: slideUp 1s;
}

input, select {
    width: 100%;
    padding: 10px;
    margin-top: 8px;
}

/* BUTTON */
.btn {
    width: 100%;
    padding: 12px;
    background: #e67e22;
    border: none;
    color: white;
    cursor: pointer;
    border-radius: 5px;
}

.btn:hover {
    background: #ca6f1e;
}

/* ANIMATION */
@keyframes slideUp {
    from {opacity:0; transform: translateY(30px);}
    to {opacity:1;}
}

/* FLOAT IMAGE */
@keyframes float {
    0% {transform: translateY(0);}
    50% {transform: translateY(-12px);}
    100% {transform: translateY(0);}
}
</style>

<div class="navbar">
    <div class="logo">🏦 PATIL CAPITALS</div>

    <div class="nav">
        <a href="/">Home</a>
        <!--  <a href="dash">Dashboard</a>-->
        <a href="login">Login</a>
        <a href="register">Sign Up</a>
        <a href="/logout">Logout</a>
    </div>
</div>