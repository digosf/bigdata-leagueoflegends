
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">
    {{--<link rel="icon" href="../../favicon.ico">--}}

    <title>Monte sua Composição Ideal - League of Legends</title>

    <link rel="stylesheet" href="{{ asset('css/core.css') }}">

    <!-- DataTables -->
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/v/bs-3.3.7/jq-2.2.4/dt-1.10.15/b-1.3.1/r-2.1.1/datatables.min.css"/>

    <link rel="stylesheet" href="{{ asset('css/site.css') }}">

    <link rel="stylesheet" href="{{ asset('js/vendor/sweetalert/sweetalert.css') }}">

    <link rel="stylesheet" href="{{ asset('css/custom.css') }}">

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    <script>
        window.Laravel = {!! json_encode([
            'csrfToken' => csrf_token(),
        ]) !!};
    </script>
</head>

<body>

<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="/">League Composições</a>
        </div>
    </div>
</nav>

@yield('content')

<script src="{{ asset('js/core.js') }}"></script>
<script type="text/javascript" src="https://cdn.datatables.net/v/bs-3.3.7/jq-2.2.4/dt-1.10.15/b-1.3.1/r-2.1.1/datatables.min.js"></script>
<script src="{{ asset('js/site.js') }}"></script>
<script src="{{ asset('js/vendor/sweetalert/sweetalert.min.js') }}"></script>
<script src="{{ asset('js/custom.js') }}"></script>
</body>
</html>
