@extends('layouts.site.master')

@section('content')
    <div class="container">
        <div class="row">
            <div class="col-md-6 col-md-offset-3">
                <div class="panel panel-default">
                    <div class="panel-heading">Campeões League of Legends</div>
                      @foreach ($champions as $champion)
                      <h4>{{$champion->name}}</h4>
                        <img src="{{$champion->link}}" alt="{{$champion->name}}" width="32px" height="32px">
                      @endforeach
                </div>
            </div>
        </div>
    </div>
@endsection
