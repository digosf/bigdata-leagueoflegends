@extends('layouts.site.master')

@section('content')
    <div class="container">
        <h2>Monte sua composição</h2>
        <div class="loading">
            <i class="fa fa-circle-o-notch fa-spin fa-3x fa-fw"></i>
            <span class="sr-only">Loading...</span>
        </div>
        <a href="#" class="btn btn-success comp-submit">Enviar composição</a>
    </div>
@endsection
