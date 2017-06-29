@extends('layouts.site.master')

@section('content')
    <div class="container">
        <!-- COMP  -->
        @foreach ($champions as $champion)
          <div class="row result-comp-row">
            <div class="col-lg-4 col-md-4 col-sm-6 col-xs-12 result-champ-col">
                <div class="result-main-box">
                    <div class="title">Para Enfrentar Campeão {{$champion['name']}}</div>
                    <img src={{$champion['image']}} class="img-responsive-champion img-circle-champion" alt="">
                </div>
            </div>
            @foreach ($content['adversary'] as $adversary)
              <div class="col-lg-4 col-md-4 col-sm-6 col-xs-10 result-champ-col">
                  <div class="result-champ-box">
                      <div class="title">Sugestão: {{$adversary['name']}}</div>
                      <img src={{$adversary['image']}} class="img-responsive img-circle" alt="">
                      <div class="win-rate-box">
                        <p><span class="rate-label bg-green">{{$adversary['value']}}%</span> de vitórias, com {{$adversary['victories']}} vitórias em {{$adversary['matches']}} partidas </p>
                      </div>
                  </div>
              </div>
            @endforeach
          </div>
        @endforeach
    </div>
@endsection
