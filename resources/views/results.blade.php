@extends('layouts.site.master')

@section('content')
    <div class="container">
        <!-- COMP  -->
        @foreach ($content as $compositions)
        <div class="row result-comp-row">
          @foreach($compositions['champions'] as $key => $champion)
            <div class="col-lg-4 col-md-4 col-sm-6 col-xs-12 result-champ-col">
                <div class="result-champ-box">
                    <div class="title">{{$champion['champion_name']}}</div>
                    <img src={{$champion['champion_image']}} class="img-responsive img-circle" alt="">
                </div>
            </div>
          @endforeach
            <div class="win-rate-box">
               <p><span class="rate-label bg-green">{{$compositions['victory_value']}}</span> vitórias</p>
            </div>
        </div>
        @endforeach
    </div>
@endsection
