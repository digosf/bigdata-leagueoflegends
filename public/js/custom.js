var apiLink = 'https://global.api.riotgames.com/api/lol/static-data/BR/v1.2/champion?champData=image&dataById=true&api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370';
var imageLink = 'http://ddragon.leagueoflegends.com/cdn/6.24.1/img/champion/';
var selectedChampions = [];

function showChampions(champions) {
    var championContainer = $('<div class="champion-container"></div>');
    var championRow = $('<div class="row"></div>');

    Object.keys(champions).forEach(function(key) {
        var championCol = $('<div class="col-lg-2 col-sm-6 col-xs-12 champion-col"></div>');
        var championBox = $('<div class="champion-box" data-id="' + champions[key].id + '"></div>');

        var title = $('<div class="title">' + champions[key].name + '</div>');
        var image = $('<img src="' + imageLink + champions[key].image.full + '" class="img-responsive img-circle" />');

        championBox.append(title).append(image);
        championCol.append(championBox);

        championRow.append(championCol);
    });

    championContainer.append(championRow);

    $('.loading').replaceWith(championContainer);
}

function validateChampionLimit() {
    if (selectedChampions.length >= 5) {
        swal({
            title: "Limite de campeões!",
            text: "Você já selecionou 5 compeões!",
            type: "error",
            confirmButtonText: "Ok"
        });

        return false;
    }

    return true;
}

$(document).ready(function () {
    $.get(apiLink)
        .done(function (response) {
            showChampions(response.data);
        })
        .fail(function () {
            console.log('Falha ao pegar dados de campeões.');
        });

    $(document).on('click', '.champion-box', function(e) {
        var el = $(this);
        var index = selectedChampions.indexOf(el.attr('data-id'));

        if (index === -1) {
            if (!validateChampionLimit()) {
                return false;
            }
            el.addClass('selected');
            selectedChampions.push(el.attr('data-id'));
        } else {
            el.removeClass('selected');
            selectedChampions.splice(index, 1);
        }
    });

    $('.comp-submit').on('click', function() {
       console.log('Enviando dados...');
    });
});