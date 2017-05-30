@extends('layouts.site.master')

@section('content')
    <div class="container">
        <h2>Resultado da composição</h2>
        <p>{!! nl2br(e($content)) !!}</p>

    </div>
@endsection
