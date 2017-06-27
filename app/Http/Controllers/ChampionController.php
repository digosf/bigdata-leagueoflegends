<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class ChampionController extends Controller
{
    public function index() {
      return view('index');
    }

    public function store(Request $request) {
        $champions = $request->get('champions');
        $fileName = 'champions_' . uniqidReal() . '.json';
        $absolutePath = storage_path($fileName);

        Storage::disk('local')->put($fileName, json_encode($champions));

        $output = shell_exec('/opt/spark-2.1.1-bin-hadoop2.7/bin/spark-submit /var/www/html/projeto-bigdata/project/loldatafinder_2.11-1.0.jar /var/www/html/projeto-bigdata/storage/app/' . $fileName . ' 2>&1');
        Storage::disk('local')->put($filename, $output);

        return json_encode(['data' => ['filename' => $fileName]]);
      }

    public function results ($id) {
       $apiLink = 'https://global.api.riotgames.com/api/lol/static-data/BR/v1.2/champion?champData=image&dataById=true&api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370';
       $imageLink = 'http://ddragon.leagueoflegends.com/cdn/6.24.1/img/champion/';
       $champion_data = json_decode(file_get_contents($apiLink), true);

        $fileName = 'app/' . $id .'.json';
        $absolutePath = storage_path($fileName);
        $json_data = json_decode(file_get_contents($absolutePath), true);

        for ($i =0; $i< count($json_data); $i ++){
          for ($j = 0; $j <5; $j ++) {
            $content[$i]['champions'][$j]['champion_name'] = $champion_data['data'][$json_data[$i][$j]]['name'];
            $content[$i]['champions'][$j]['champion_image'] = $imageLink . $champion_data['data'][$json_data[$i][$j]]['image']['full'];
          }
            $content[$i]['victory_value'] = $json_data[$i][$j];
        }
        //dd($content[1]['champions']);
        return view ('results', ['content' => $content]);
    }
}
