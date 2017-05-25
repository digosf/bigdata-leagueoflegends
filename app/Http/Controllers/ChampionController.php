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
    public function index()
    {
      $json = json_decode(file_get_contents('https://br1.api.riotgames.com/lol/static-data/v3/champions?dataById=true&api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370'), true);
      dd($json);

    }
}
