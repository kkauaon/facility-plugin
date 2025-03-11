## Tutorial de configuração do plugin
- Aviso: O plugin só suporta uma partida simultânea, em um mapa apenas. Feito exclusivamente para jogar com amigos.

### Passo 1: Definir os Spawns
Você deve definir o lobby, player spawn e o beast spawn.

`/facility setlobby` (no lugar de espera, lobby)

`/facility setplayerspawn` (no mapa)

`/facility setbeastspawn` (o lugar em que a besta ficará durante os 15 segundos iniciais)


### Passo 2: Definir os PCs para hackear
Uma regra é que deverá ter um bloco e na frente desse bloco um botão posicionado um bloco a baixo, como se fosse uma mesa com o mouse. O botão aponta para o bloco que será a tela, e deve ficar obrigatoriamente no bloco seguido da tela.

`/facility setpcbutton <número>` (olhando para o botão)

`/facility setpcscreen <número>` (olhando para o bloco tela)

Atenção: o botão e a tela devem ter o mesmo número, ou seja, se for definir o PC 1 por exemplo, terá de usar `setpcbutton 1` e `setpcscreen 1`.

### Passo 3: Definir os congeladores
Construa os congeladores para que o player consiga ficar preso dentro dele, coloque também um botão fora para ser usado para descongelar.
`/facility setfreezerbutton <número>` (olhando para o botão)

`/facility setfreezerplayer <número>` (você deve estar posicionado exatamente no lugar onde o player ficará enquanto congelando)

### Passo 4: Definir número de PCs para hackear na partida
`/facility setpctohack <número de PCs>`

Atenção: é interessante que esse número se adapte ao número de jogadores que irão participar do jogo, para ajustar a dificuldade.

### Configuração adicional (opcional)
Você pode configurar a distância em que os PCs podem ser hackeados usando `/facility setpchackdistance <Distância>`. O padrão é `6`, mas você pode ajustar para o que achar melhor.

### Passo 5: Jogar
Toda vez que quiser iniciar uma partida, use `/facility start <USERNAME DA BESTA>`, substitua o `<USERNAME DA BESTA>` pelo nick da pessoa que será a besta dessa partida.

Uma vez iniciado, não tente iniciar outro jogo.

O jogo não termina sozinho, então assim que todos forem congelados, ou todos escaparem, use o comando `/facility stop` e o jogo parará e todos serão teleportados para o lobby.
