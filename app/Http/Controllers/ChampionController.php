<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class ChampionController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */

     public function getChampions () {

       $json = json_decode(file_get_contents('https://global.api.riotgames.com/api/lol/static-data/BR/v1.2/champion?champData=image&dataById=true&api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370'), false);

       foreach ($json->data as $champion) {
         $champion->link = $this->getImage($champion->image->full);

       }

       return $json->data;

     }

     public function getImage ($name) {
       $img = 'http://ddragon.leagueoflegends.com/cdn/6.24.1/img/champion/' . $name;
       return $img;
     }



    public function index()
    {
      $champions = $this->getChampions();
      return view('index', ['champions' => $champions]);

    }


}
