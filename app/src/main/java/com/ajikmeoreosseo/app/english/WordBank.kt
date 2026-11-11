package com.ajikmeoreosseo.app.english

/** 차의 움직임과 묶인 말. 그 움직임이 실제로 일어난 순간에 가르칩니다. */
enum class Cue { LEFT, RIGHT, STOP, GO, RAIN, BELT, BACK, HOW_FAR }

/** 말의 종류. 문장을 만들 때([Sentences]) 어떤 자리에 들어갈 수 있는지 정합니다. */
enum class Tag { NOUN, MASS, ADJ, COLOR, VERB, NUMBER, PHRASE, SENTENCE }

/**
 * 가르칠 영어 한 가지.
 *
 * @param level 1 낱말, 2 짧은 표현, 3 문장
 * @param cue 차의 움직임과 묶인 말이면 그 움직임. 없으면 동화 사이사이에 가르칩니다.
 * @param tag 말의 종류. MASS는 셀 수 없는 명사(a를 붙이지 않음).
 * @param attr 꾸밈말로 쓸 때의 우리말(큰, 빨간). 형용사와 색깔만.
 * @param pred 서술어로 쓸 때의 우리말(커요). 형용사만.
 * @param topic 낱말의 주제(동물, 먹을 것 …). 레벨 1 낱말에만 있고, 새 말을 고를 때 주제를 돌아가며 내는 데 씁니다([WordPicker]).
 */
data class Word(
    val id: String,
    val english: String,
    val korean: String,
    val level: Int,
    val cue: Cue? = null,
    val tag: Tag = Tag.PHRASE,
    val attr: String = "",
    val pred: String = "",
    val topic: String = "",
)

/**
 * 영어 단어장.
 *
 * 레벨 1 낱말은 아래 [VOCABULARY]에 "영어|우리말|종류[|꾸밈말|서술어]" 꼴로 적혀 있고 id는 영어를 소문자로 바꾼 것입니다
 * (이야기에 나온 말 → 창밖 → 동물 → 먹을 것 → 몸·옷 → 집·장난감 → 자연·장소 → 탈것 → 사람 → 색·수 → 형용사 → 동사 → 때·학교).
 * 레벨 2·3은 손으로 쓴 표현과 문장이고, 그 뒤로는 익힌 낱말을 엮어 만든 문장([Sentences])이 끝없이 이어집니다.
 * 말을 더하거나 고치려면 이 파일만 바꾸면 됩니다(id는 진도 저장에 쓰이니 바꾸지 마세요).
 */
object WordBank {
    /** 말의 종류는 셋(1 낱말, 2 짧은 표현, 3 문장). 난이도 다섯 단계는 [Levels]가 이 종류와 주제로 정합니다. */
    const val MAX_LEVEL = 3

    private const val VOCABULARY = """
# 이야기에 나오는 것
Puppy|강아지|n
Moon|달|n
Star|별|n
Cloud|구름|n
Sky|하늘|n
Fish|물고기|n
Friend|친구|n
Tail|꼬리|n
Bone|뼈다귀|n
Tunnel|터널|n
Wind|바람|m
Hill|언덕|n
Island|섬|n
Dream|꿈|n
Night|밤|n
Window|창문|n
Rainbow|무지개|n
Car|자동차|n
Hello|안녕|p
Treat|간식|n
Wish|소원|n
Adventure|모험|n
Map|지도|n
Treasure|보물|n
Magic|마법|m
Secret|비밀|n
Bell|종|n
Feather|깃털|n
Wing|날개|n
Paw|발바닥|n
Collar|목걸이(강아지)|n
Lantern|등불|n
Path|오솔길|n
# 창밖에 보이는 것
Sun|해|n
Tree|나무|n
Flower|꽃|n
Bird|새|n
Dog|개|n
Cat|고양이|n
Road|길|n
Bus|버스|n
Truck|트럭|n
Bicycle|자전거|n
Bridge|다리|n
Light|불빛|n
House|집|n
School|학교|n
Mountain|산|n
River|강|n
Sea|바다|n
Snow|눈|m
Door|문|n
Seat|좌석|n
Train|기차|n
Airplane|비행기|n
Boat|배(탈것)|n
Taxi|택시|n
Ambulance|구급차|n
Fire truck|소방차|n
Police car|경찰차|n
Helicopter|헬리콥터|n
Scooter|킥보드|n
Wheel|바퀴|n
Traffic light|신호등|n
Sign|표지판|n
Park|공원|n
Shop|가게|n
Market|시장|n
Hospital|병원|n
Library|도서관|n
Playground|놀이터|n
Station|역|n
Airport|공항|n
Zoo|동물원|n
Farm|농장|n
City|도시|n
Village|마을|n
Building|건물|n
Apartment|아파트|n
Tower|탑|n
Castle|성|n
Fountain|분수|n
Statue|동상|n
Flag|깃발|n
Bench|벤치|n
Fence|울타리|n
Gate|대문|n
Street|거리|n
Corner|모퉁이|n
Crosswalk|횡단보도|n
Sidewalk|인도|n
Bakery|빵집|n
Restaurant|식당|n
Cafe|카페|n
Supermarket|마트|n
Bookstore|서점|n
Toy store|장난감 가게|n
Pharmacy|약국|n
Bank|은행|n
Post office|우체국|n
Police station|경찰서|n
Fire station|소방서|n
Museum|박물관|n
Theater|극장|n
Aquarium|수족관|n
Amusement park|놀이공원|n
Swimming pool|수영장|n
Stadium|경기장|n
Hotel|호텔|n
Kindergarten|유치원|n
Factory|공장|n
Windmill|풍차|n
Lighthouse|등대|n
Harbor|항구|n
Campsite|캠핑장|n
Streetlight|가로등|n
Mailbox|우체통|n
Bus stop|버스 정류장|n
Chimney|굴뚝|n
# 동물
Rabbit|토끼|n
Bear|곰|n
Lion|사자|n
Tiger|호랑이|n
Elephant|코끼리|n
Monkey|원숭이|n
Giraffe|기린|n
Zebra|얼룩말|n
Horse|말|n
Cow|소|n
Pig|돼지|n
Sheep|양|n
Duck|오리|n
Chicken|닭|n
Frog|개구리|n
Turtle|거북이|n
Snake|뱀|n
Mouse|쥐|n
Fox|여우|n
Wolf|늑대|n
Deer|사슴|n
Panda|판다|n
Penguin|펭귄|n
Whale|고래|n
Dolphin|돌고래|n
Shark|상어|n
Octopus|문어|n
Crab|게|n
Bee|벌|n
Butterfly|나비|n
Ant|개미|n
Ladybug|무당벌레|n
Spider|거미|n
Owl|부엉이|n
Parrot|앵무새|n
Squirrel|다람쥐|n
Hamster|햄스터|n
Goat|염소|n
Hippo|하마|n
Kangaroo|캥거루|n
Dinosaur|공룡|n
Camel|낙타|n
Cheetah|치타|n
Leopard|표범|n
Gorilla|고릴라|n
Koala|코알라|n
Raccoon|너구리|n
Hedgehog|고슴도치|n
Bat|박쥐|n
Eagle|독수리|n
Swan|백조|n
Goose|거위|n
Peacock|공작새|n
Flamingo|홍학|n
Pigeon|비둘기|n
Crow|까마귀|n
Sparrow|참새|n
Magpie|까치|n
Seal|물개|n
Otter|수달|n
Jellyfish|해파리|n
Starfish|불가사리|n
Shrimp|새우|n
Squid|오징어|n
Lobster|바닷가재|n
Snail|달팽이|n
Worm|지렁이|n
Dragonfly|잠자리|n
Mosquito|모기|n
Caterpillar|애벌레|n
Grasshopper|메뚜기|n
Cricket|귀뚜라미|n
Beetle|딱정벌레|n
Firefly|반딧불이|n
Lizard|도마뱀|n
Crocodile|악어|n
Chameleon|카멜레온|n
Donkey|당나귀|n
Pony|조랑말|n
Lamb|아기 양|n
Kitten|아기 고양이|n
Chick|병아리|n
Calf|송아지|n
Rooster|수탉|n
Turkey|칠면조|n
Rhino|코뿔소|n
Polar bear|북극곰|n
Walrus|바다코끼리|n
Reindeer|순록|n
Beaver|비버|n
Mole|두더지|n
Ostrich|타조|n
Woodpecker|딱따구리|n
Seagull|갈매기|n
Goldfish|금붕어|n
Seahorse|해마|n
Clam|조개|n
Sloth|나무늘보|n
Meerkat|미어캣|n
Chimpanzee|침팬지|n
Tadpole|올챙이|n
Nest|둥지|n
Beak|부리|n
Fin|지느러미|n
Web|거미줄|n
Hive|벌집|n
Pet|반려동물|n
Insect|곤충|n
Animal|동물|n
# 먹을 것
Apple|사과|n
Banana|바나나|n
Grape|포도|n
Strawberry|딸기|n
Orange|오렌지|n
Watermelon|수박|n
Peach|복숭아|n
Pear|배(과일)|n
Lemon|레몬|n
Carrot|당근|n
Potato|감자|n
Tomato|토마토|n
Corn|옥수수|n
Onion|양파|n
Bread|빵|m
Rice|밥|m
Egg|달걀|n
Cheese|치즈|m
Pizza|피자|n
Cake|케이크|n
Cookie|과자|n
Candy|사탕|n
Chocolate|초콜릿|m
Ice cream|아이스크림|m
Milk|우유|m
Juice|주스|m
Water|물|m
Soup|수프|m
Noodle|국수|n
Sandwich|샌드위치|n
Hamburger|햄버거|n
Honey|꿀|m
Jam|잼|m
Salt|소금|m
Sugar|설탕|m
Butter|버터|m
Yogurt|요구르트|m
Mango|망고|n
Pineapple|파인애플|n
Kiwi|키위|n
Cherry|체리|n
Blueberry|블루베리|n
Melon|멜론|n
Coconut|코코넛|n
Plum|자두|n
Persimmon|감|n
Tangerine|귤|n
Cucumber|오이|n
Pumpkin|호박|n
Sweet potato|고구마|n
Cabbage|양배추|n
Lettuce|상추|m
Spinach|시금치|m
Broccoli|브로콜리|m
Mushroom|버섯|n
Garlic|마늘|m
Pepper|고추|n
Bean|콩|n
Radish|무|n
Peanut|땅콩|n
Walnut|호두|n
Chestnut|밤(먹는 것)|n
Meat|고기|m
Sausage|소시지|n
Ham|햄|m
Tofu|두부|m
Kimchi|김치|m
Dumpling|만두|n
Rice cake|떡|n
Seaweed|김|m
Pancake|팬케이크|n
Waffle|와플|n
Donut|도넛|n
Pie|파이|n
Cereal|시리얼|m
Popcorn|팝콘|m
Jelly|젤리|n
Pudding|푸딩|n
Lollipop|막대 사탕|n
Tea|차(마시는 것)|m
Cocoa|코코아|m
Salad|샐러드|n
Spaghetti|스파게티|m
Curry|카레|m
French fries|감자튀김|m
Toast|토스트|m
Breakfast|아침밥|m
Lunch|점심밥|m
Dinner|저녁밥|m
Lunchbox|도시락|n
Gimbap|김밥|m
Fried rice|볶음밥|m
Ramen|라면|m
Fried chicken|치킨|m
Ketchup|케첩|m
Fruit|과일|n
Vegetable|채소|n
Straw|빨대|n
Chopsticks|젓가락|m
Kettle|주전자|n
Pot|냄비|n
# 몸과 옷
Hand|손|n
Foot|발|n
Eye|눈(얼굴)|n
Ear|귀|n
Nose|코|n
Mouth|입|n
Head|머리|n
Hair|머리카락|m
Face|얼굴|n
Arm|팔|n
Leg|다리(몸)|n
Finger|손가락|n
Toe|발가락|n
Knee|무릎|n
Tummy|배(몸)|n
Tooth|이(치아)|n
Shoulder|어깨|n
Neck|목|n
Hat|모자|n
Shirt|셔츠|n
Pants|바지|m
Dress|원피스|n
Shoe|신발|n
Sock|양말|n
Coat|코트|n
Jacket|재킷|n
Glove|장갑|n
Scarf|목도리|n
Button|단추|n
Pocket|주머니|n
Elbow|팔꿈치|n
Wrist|손목|n
Ankle|발목|n
Chest|가슴|n
Tongue|혀|n
Lip|입술|n
Cheek|볼|n
Chin|턱|n
Forehead|이마|n
Eyebrow|눈썹|n
Thumb|엄지손가락|n
Skin|피부|m
Body|몸|n
Voice|목소리|n
Tear|눈물|n
Skirt|치마|n
Sweater|스웨터|n
T-shirt|티셔츠|n
Shorts|반바지|m
Pajamas|잠옷|m
Uniform|유니폼|n
Boot|장화|n
Sandal|샌들|n
Slipper|슬리퍼|n
Mitten|벙어리장갑|n
Cap|야구 모자|n
Belt|허리띠|n
Ribbon|리본|n
Glasses|안경|m
Sunglasses|선글라스|m
Mask|마스크|n
Raincoat|비옷|n
Umbrella|우산|n
Swimsuit|수영복|n
Zipper|지퍼|n
Necklace|목걸이|n
Ring|반지|n
Bracelet|팔찌|n
Watch|손목시계|n
Backpack|책가방|n
Helmet|헬멧|n
Apron|앞치마|n
Clothes|옷|m
Handkerchief|손수건|n
# 집과 장난감
Bed|침대|n
Chair|의자|n
Table|식탁|n
Desk|책상|n
Lamp|램프|n
Clock|시계|n
Cup|컵|n
Plate|접시|n
Spoon|숟가락|n
Fork|포크|n
Bowl|그릇|n
Bottle|병|n
Bag|가방|n
Box|상자|n
Key|열쇠|n
Phone|전화기|n
TV|텔레비전|n
Computer|컴퓨터|n
Picture|그림|n
Mirror|거울|n
Pillow|베개|n
Blanket|이불|n
Towel|수건|n
Soap|비누|m
Toothbrush|칫솔|n
Kitchen|부엌|n
Bathroom|화장실|n
Room|방|n
Roof|지붕|n
Garden|정원|n
Stairs|계단|m
Wall|벽|n
Floor|바닥|n
Toy|장난감|n
Doll|인형|n
Robot|로봇|n
Kite|연|n
Balloon|풍선|n
Puzzle|퍼즐|n
Crayon|크레용|n
Paper|종이|m
Pencil|연필|n
Eraser|지우개|n
Scissors|가위|m
Drum|북|n
Piano|피아노|n
Guitar|기타|n
Swing|그네|n
Slide|미끄럼틀|n
Block|블록|n
Ball|공|n
Book|책|n
Gift|선물|n
Sofa|소파|n
Carpet|카펫|n
Curtain|커튼|n
Shelf|선반|n
Drawer|서랍|n
Closet|옷장|n
Refrigerator|냉장고|n
Oven|오븐|n
Microwave|전자레인지|n
Sink|싱크대|n
Bathtub|욕조|n
Shower|샤워기|n
Toilet|변기|n
Faucet|수도꼭지|n
Fan|선풍기|n
Heater|난로|n
Air conditioner|에어컨|n
Vacuum|청소기|n
Washing machine|세탁기|n
Basket|바구니|n
Bucket|양동이|n
Broom|빗자루|n
Trash can|쓰레기통|n
Candle|양초|n
Vase|꽃병|n
Calendar|달력|n
Envelope|편지 봉투|n
Stamp|우표|n
Card|카드|n
Tape|테이프|m
Glue|풀(붙이는 것)|m
Ruler|자|n
Notebook|공책|n
Sketchbook|스케치북|n
Paint|물감|m
Brush|붓|n
Clay|찰흙|m
Marble|구슬|n
Top|팽이|n
Jump rope|줄넘기|n
Teddy bear|곰 인형|n
Dice|주사위|n
Tent|텐트|n
Flashlight|손전등|n
Remote|리모컨|n
Camera|카메라|n
Radio|라디오|n
Headphones|헤드폰|m
Tablet|태블릿|n
Light bulb|전구|n
Switch|스위치|n
Doorbell|초인종|n
Garage|차고|n
Yard|마당|n
Elevator|엘리베이터|n
Ladder|사다리|n
Hammer|망치|n
Rope|밧줄|n
Comb|빗|n
Toothpaste|치약|m
Shampoo|샴푸|m
Tissue|휴지|m
Bookshelf|책장|n
Frame|액자|n
Trophy|트로피|n
Medal|메달|n
Sticker|스티커|n
Magnet|자석|n
Pinwheel|바람개비|n
Seesaw|시소|n
Trampoline|트램펄린|n
Board game|보드게임|n
Xylophone|실로폰|n
Violin|바이올린|n
Flute|피리|n
Trumpet|트럼펫|n
Tambourine|탬버린|n
Microphone|마이크|n
# 자연
Grass|풀|m
Leaf|나뭇잎|n
Rock|돌|n
Sand|모래|m
Beach|바닷가|n
Forest|숲|n
Lake|호수|n
Field|들판|n
Storm|폭풍|n
Fog|안개|m
Ice|얼음|m
Fire|불|m
Earth|땅|m
World|세상|n
Space|우주|m
Rocket|로켓|n
Planet|행성|n
Heart|하트|n
Circle|동그라미|n
Triangle|세모|n
Square|네모|n
Line|줄|n
Hole|구멍|n
Shadow|그림자|n
Thunder|천둥|m
Lightning|번개|m
Snowman|눈사람|n
Snowflake|눈송이|n
Puddle|물웅덩이|n
Sunshine|햇살|m
Sunrise|해돋이|n
Sunset|노을|n
Moonlight|달빛|m
Weather|날씨|m
Season|계절|n
Rose|장미|n
Tulip|튤립|n
Sunflower|해바라기|n
Dandelion|민들레|n
Cherry blossom|벚꽃|n
Pine tree|소나무|n
Bamboo|대나무|n
Seed|씨앗|n
Root|뿌리|n
Branch|나뭇가지|n
Acorn|도토리|n
Pine cone|솔방울|n
Bush|덤불|n
Cactus|선인장|n
Shell|조개껍데기|n
Cave|동굴|n
Cliff|절벽|n
Valley|골짜기|n
Waterfall|폭포|n
Pond|연못|n
Stream|시냇물|n
Desert|사막|n
Jungle|정글|n
Volcano|화산|n
Mud|진흙|m
Dust|먼지|m
Pebble|조약돌|n
Soil|흙|m
Air|공기|m
Comet|혜성|n
Galaxy|은하수|n
Telescope|망원경|n
Compass|나침반|n
Globe|지구본|n
Country|나라|n
Ocean|큰 바다|n
Meadow|풀밭|n
Petal|꽃잎|n
Clover|클로버|n
Dew|이슬|m
Icicle|고드름|n
Breeze|산들바람|n
Typhoon|태풍|n
Shade|그늘|n
Plant|식물|n
# 차와 길
Steering wheel|운전대|n
Wiper|와이퍼|n
Headlight|전조등|n
Horn|경적|n
Engine|엔진|n
Tire|타이어|n
Trunk|트렁크|n
Brake|브레이크|n
Navigation|내비게이션|n
Driver|운전사|n
Passenger|승객|n
Highway|고속도로|n
Tollgate|톨게이트|n
Rest area|휴게소|n
Gas station|주유소|n
Charger|충전기|n
Battery|배터리|n
Electric car|전기차|n
Parking lot|주차장|n
Car wash|세차장|n
Traffic jam|교통 체증|m
Speed|속도|m
Lane|차선|n
Intersection|사거리|n
Speed bump|과속 방지턱|n
License plate|번호판|n
Car seat|카시트|n
Subway|지하철|n
Motorcycle|오토바이|n
Van|승합차|n
Ship|큰 배|n
Submarine|잠수함|n
Tractor|트랙터|n
Excavator|굴착기|n
Garbage truck|청소차|n
Tow truck|견인차|n
Sled|썰매|n
Hot air balloon|열기구|n
Spaceship|우주선|n
Ferry|여객선|n
Sailboat|돛단배|n
Skateboard|스케이트보드|n
Stroller|유모차|n
Cable car|케이블카|n
Crane|크레인|n
Bulldozer|불도저|n
School bus|스쿨버스|n
Race car|경주차|n
# 공룡과 상상
Dragon|용|n
Unicorn|유니콘|n
Fairy|요정|n
Mermaid|인어|n
Giant|거인|n
Wizard|마법사|n
Pirate|해적|n
Knight|기사|n
Alien|외계인|n
Angel|천사|n
Superhero|슈퍼히어로|n
Crown|왕관|n
Wand|요술봉|n
Palace|궁전|n
Fossil|화석|n
Footprint|발자국|n
Santa|산타 할아버지|n
Puppet|손인형|n
Bubble|비눗방울|n
Fireworks|불꽃놀이|m
Parade|퍼레이드|n
Circus|서커스|n
Clown|광대|n
Magician|마술사|n
Carousel|회전목마|n
Ferris wheel|대관람차|n
Roller coaster|롤러코스터|n
# 사람
Mom|엄마|n
Dad|아빠|n
Baby|아기|n
Grandma|할머니|n
Grandpa|할아버지|n
Teacher|선생님|n
Doctor|의사|n
Boy|남자아이|n
Girl|여자아이|n
Family|가족|n
King|왕|n
Queen|여왕|n
Princess|공주|n
Prince|왕자|n
Brother|형제|n
Sister|자매|n
Uncle|삼촌|n
Aunt|이모|n
Cousin|사촌|n
Twin|쌍둥이|n
Child|어린이|n
Adult|어른|n
Neighbor|이웃|n
Guest|손님|n
Firefighter|소방관|n
Police officer|경찰관|n
Nurse|간호사|n
Dentist|치과 의사|n
Vet|수의사|n
Chef|요리사|n
Baker|제빵사|n
Farmer|농부|n
Pilot|조종사|n
Sailor|선원|n
Astronaut|우주 비행사|n
Singer|가수|n
Dancer|무용수|n
Artist|화가|n
Writer|작가|n
Scientist|과학자|n
Mail carrier|집배원|n
Fisherman|어부|n
Athlete|운동선수|n
Student|학생|n
Classmate|반 친구|n
Hero|영웅|n
Captain|선장|n
Person|사람|n
Team|팀|n
Parent|부모님|n
Son|아들|n
Daughter|딸|n
Librarian|사서|n
Zookeeper|사육사|n
Explorer|탐험가|n
Inventor|발명가|n
Musician|음악가|n
# 색깔
Red|빨강|c|빨간
Blue|파랑|c|파란
Yellow|노랑|c|노란
Green|초록|c|초록
Purple|보라|c|보라
Pink|분홍|c|분홍
Black|검정|c|검은
White|하양|c|하얀
Brown|갈색|c|갈색
Gray|회색|c|회색
Gold|금색|c|금색
Silver|은색|c|은색
Sky blue|하늘색|c|하늘색
Navy|남색|c|남색
Light green|연두색|c|연두색
# 수
One|하나|num
Two|둘|num
Three|셋|num
Four|넷|num
Five|다섯|num
Six|여섯|num
Seven|일곱|num
Eight|여덟|num
Nine|아홉|num
Ten|열|num
Zero|영|num
Eleven|열하나|num
Twelve|열둘|num
Thirteen|열셋|num
Fourteen|열넷|num
Fifteen|열다섯|num
Twenty|스물|num
Thirty|서른|num
Fifty|쉰|num
Hundred|백|num
Thousand|천|num
First|첫째|num
Second|둘째|num
Third|셋째|num
Half|절반|n
Pair|한 쌍|n
# 형용사
Big|크다|a|큰|커요
Small|작다|a|작은|작아요
Happy|행복하다|a|행복한|행복해요
Sleepy|졸리다|a|졸린|졸려요
Hungry|배고프다|a|배고픈|배고파요
Fast|빠르다|a|빠른|빨라요
Slow|느리다|a|느린|느려요
Hot|뜨겁다|a|뜨거운|뜨거워요
Cold|차갑다|a|차가운|차가워요
Soft|부드럽다|a|부드러운|부드러워요
Hard|딱딱하다|a|딱딱한|딱딱해요
Long|길다|a|긴|길어요
Short|짧다|a|짧은|짧아요
Tall|키가 크다|a|키 큰|키가 커요
New|새롭다|a|새|새로워요
Old|오래되다|a|오래된|오래됐어요
Clean|깨끗하다|a|깨끗한|깨끗해요
Dirty|더럽다|a|더러운|더러워요
Loud|시끄럽다|a|시끄러운|시끄러워요
Quiet|조용하다|a|조용한|조용해요
Pretty|예쁘다|a|예쁜|예뻐요
Cute|귀엽다|a|귀여운|귀여워요
Funny|웃기다|a|웃긴|웃겨요
Sad|슬프다|a|슬픈|슬퍼요
Angry|화나다|a|화난|화났어요
Scared|무섭다|a|무서운|무서워요
Tired|피곤하다|a|피곤한|피곤해요
Sweet|달다|a|단|달아요
Round|둥글다|a|둥근|둥글어요
Wet|젖다|a|젖은|젖었어요
Dry|마르다|a|마른|말랐어요
Heavy|무겁다|a|무거운|무거워요
Full|가득하다|a|가득한|가득해요
Empty|비다|a|빈|비었어요
Strong|힘세다|a|힘센|힘세요
Bright|밝다|a|밝은|밝아요
Dark|어둡다|a|어두운|어두워요
Warm|따뜻하다|a|따뜻한|따뜻해요
Sunny|맑다|a|맑은|맑아요
Windy|바람이 불다|a|바람 부는|바람이 불어요
Good|좋다|a|좋은|좋아요
Bad|나쁘다|a|나쁜|나빠요
Nice|멋지다|a|멋진|멋져요
Kind|친절하다|a|친절한|친절해요
Brave|용감하다|a|용감한|용감해요
Shy|수줍다|a|수줍은|수줍어요
Smart|똑똑하다|a|똑똑한|똑똑해요
Silly|엉뚱하다|a|엉뚱한|엉뚱해요
Lazy|게으르다|a|게으른|게을러요
Busy|바쁘다|a|바쁜|바빠요
Gentle|다정하다|a|다정한|다정해요
Smooth|매끄럽다|a|매끄러운|매끄러워요
Sharp|뾰족하다|a|뾰족한|뾰족해요
Sticky|끈적하다|a|끈적한|끈적해요
Slippery|미끄럽다|a|미끄러운|미끄러워요
Fluffy|폭신하다|a|폭신한|폭신해요
Bumpy|울퉁불퉁하다|a|울퉁불퉁한|울퉁불퉁해요
Shiny|반짝이다|a|반짝이는|반짝여요
Colorful|알록달록하다|a|알록달록한|알록달록해요
Thick|두껍다|a|두꺼운|두꺼워요
Thin|얇다|a|얇은|얇아요
Wide|넓다|a|넓은|넓어요
Narrow|좁다|a|좁은|좁아요
Deep|깊다|a|깊은|깊어요
High|높다|a|높은|높아요
Low|낮다|a|낮은|낮아요
Near|가깝다|a|가까운|가까워요
Far|멀다|a|먼|멀어요
Young|어리다|a|어린|어려요
Tiny|아주 작다|a|아주 작은|아주 작아요
Huge|아주 크다|a|아주 큰|아주 커요
Thirsty|목마르다|a|목마른|목말라요
Sick|아프다|a|아픈|아파요
Healthy|건강하다|a|건강한|건강해요
Itchy|간지럽다|a|간지러운|간지러워요
Excited|신나다|a|신난|신나요
Surprised|놀라다|a|놀란|놀랐어요
Bored|심심하다|a|심심한|심심해요
Lonely|외롭다|a|외로운|외로워요
Proud|자랑스럽다|a|자랑스러운|자랑스러워요
Calm|차분하다|a|차분한|차분해요
Cool|시원하다|a|시원한|시원해요
Cozy|포근하다|a|포근한|포근해요
Fresh|신선하다|a|신선한|신선해요
Delicious|맛있다|a|맛있는|맛있어요
Salty|짜다|a|짠|짜요
Sour|시다|a|신|셔요
Spicy|맵다|a|매운|매워요
Crunchy|바삭하다|a|바삭한|바삭해요
Easy|쉽다|a|쉬운|쉬워요
Difficult|어렵다|a|어려운|어려워요
Same|같다|a|같은|같아요
Different|다르다|a|다른|달라요
Safe|안전하다|a|안전한|안전해요
Dangerous|위험하다|a|위험한|위험해요
Lucky|운이 좋다|a|운이 좋은|운이 좋아요
Special|특별하다|a|특별한|특별해요
Beautiful|아름답다|a|아름다운|아름다워요
Wonderful|훌륭하다|a|훌륭한|훌륭해요
Rainy|비가 오다|a|비 오는|비가 와요
Snowy|눈이 오다|a|눈 오는|눈이 와요
Cloudy|흐리다|a|흐린|흐려요
Chilly|쌀쌀하다|a|쌀쌀한|쌀쌀해요
Curious|궁금하다|a|궁금한|궁금해요
Friendly|상냥하다|a|상냥한|상냥해요
Cheerful|명랑하다|a|명랑한|명랑해요
Messy|지저분하다|a|지저분한|지저분해요
Tidy|깔끔하다|a|깔끔한|깔끔해요
Curly|곱슬곱슬하다|a|곱슬곱슬한|곱슬곱슬해요
Flat|납작하다|a|납작한|납작해요
# 동사
Run|달려요|v
Jump|뛰어요|v
Walk|걸어요|v
Sit|앉아요|v
Stand|서요|v
Sleep|자요|v
Eat|먹어요|v
Drink|마셔요|v
Play|놀아요|v
Sing|노래해요|v
Dance|춤춰요|v
Read|읽어요|v
Draw|그려요|v
Swim|수영해요|v
Fly|날아요|v
Open|열어요|v
Close|닫아요|v
Push|밀어요|v
Pull|당겨요|v
Wash|씻어요|v
Look|봐요|v
Listen|들어요|v
Smile|웃어요|v
Cry|울어요|v
Hug|안아요|v
Kiss|뽀뽀해요|v
Clap|손뼉 쳐요|v
Wave|손을 흔들어요|v
Climb|올라가요|v
Catch|잡아요|v
Throw|던져요|v
Give|줘요|v
Help|도와줘요|v
Say|말해요|v
Think|생각해요|v
Laugh|깔깔 웃어요|v
Come|와요|v
Start|시작해요|v
Finish|끝내요|v
Hurry|서둘러요|v
Make|만들어요|v
Build|지어요|v
Cut|잘라요|v
Fold|접어요|v
Write|써요|v
Count|세요|v
Learn|배워요|v
Teach|가르쳐요|v
Know|알아요|v
Remember|기억해요|v
Like|좋아해요|v
Love|사랑해요|v
Want|원해요|v
Need|필요해요|v
Find|찾아요|v
Hide|숨어요|v
Show|보여 줘요|v
Tell|알려 줘요|v
Ask|물어봐요|v
Call|불러요|v
Shout|소리쳐요|v
Whisper|속삭여요|v
Talk|이야기해요|v
Touch|만져요|v
Feel|느껴요|v
Smell|냄새 맡아요|v
Taste|맛봐요|v
Bite|깨물어요|v
Blow|불어요|v
Breathe|숨 쉬어요|v
Sneeze|재채기해요|v
Yawn|하품해요|v
Wake up|일어나요|v
Rest|쉬어요|v
Wear|입어요|v
Carry|들고 가요|v
Hold|잡고 있어요|v
Drop|떨어뜨려요|v
Pick up|주워요|v
Bring|가져와요|v
Share|나눠요|v
Buy|사요|v
Cook|요리해요|v
Bake|구워요|v
Mix|섞어요|v
Pour|부어요|v
Wipe|닦아요|v
Dig|파요|v
Grow|자라요|v
Feed|먹이를 줘요|v
Ride|타요|v
Drive|운전해요|v
Turn|돌아요|v
Cross|건너요|v
Follow|따라가요|v
Kick|차요|v
Roll|굴러요|v
Bounce|통통 튀어요|v
Spin|빙글 돌아요|v
Hop|깡충 뛰어요|v
Crawl|기어가요|v
Stretch|쭉 뻗어요|v
Shake|흔들어요|v
Nod|끄덕여요|v
Point|가리켜요|v
Knock|두드려요|v
Fix|고쳐요|v
Win|이겨요|v
Cheer|응원해요|v
Visit|찾아가요|v
Travel|여행해요|v
Arrive|도착해요|v
Meet|만나요|v
Tickle|간지럽혀요|v
Giggle|킥킥 웃어요|v
Imagine|상상해요|v
Choose|골라요|v
Guess|맞혀 봐요|v
Float|둥둥 떠요|v
Splash|첨벙거려요|v
Shine|빛나요|v
Twinkle|반짝여요|v
Bark|멍멍 짖어요|v
Wag|꼬리를 흔들어요|v
Sniff|킁킁 냄새 맡아요|v
# 때와 학교
Morning|아침|n
Afternoon|오후|n
Evening|저녁|n
Today|오늘|n
Tomorrow|내일|n
Spring|봄|n
Summer|여름|n
Fall|가을|n
Winter|겨울|n
Birthday|생일|n
Holiday|휴일|n
Week|일주일|n
Classroom|교실|n
Letter|글자|n
Number|숫자|n
Name|이름|n
Question|질문|n
Answer|대답|n
Story|이야기|n
Music|음악|m
Art|미술|m
Homework|숙제|m
Song|노래|n
Money|돈|m
Noise|소리|m
Day|낮|n
Noon|한낮|n
Yesterday|어제|n
Weekend|주말|n
Month|한 달|n
Year|한 해|n
Hour|한 시간|n
Minute|일 분|n
Time|시간|m
Monday|월요일|n
Tuesday|화요일|n
Wednesday|수요일|n
Thursday|목요일|n
Friday|금요일|n
Saturday|토요일|n
Sunday|일요일|n
Blackboard|칠판|n
Lesson|수업|n
Nap|낮잠|n
Field trip|소풍|n
Picnic|나들이|n
Festival|축제|n
Party|파티|n
Vacation|방학|n
Trip|여행|n
Camping|캠핑|m
Christmas|크리스마스|n
New Year|새해|n
Game|놀이|n
Soccer|축구|m
Baseball|야구|m
Basketball|농구|m
Skating|스케이트|m
Taekwondo|태권도|m
Race|달리기 시합|n
Hide-and-seek|숨바꼭질|m
Movie|영화|n
Cartoon|만화|n
Fairy tale|동화|n
Word|낱말|n
Idea|생각|n
Rule|규칙|n
Promise|약속|n
Prize|상|n
Shape|모양|n
Size|크기|n
"""

    private fun vocabulary(): List<Word> {
        var topic = ""
        return VOCABULARY.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            if (line.startsWith("#")) {
                topic = line.removePrefix("#").trim()
                return@mapNotNull null
            }
            val f = line.split('|')
            val tag = when (f[2]) {
                "n" -> Tag.NOUN; "m" -> Tag.MASS; "a" -> Tag.ADJ; "c" -> Tag.COLOR
                "v" -> Tag.VERB; "num" -> Tag.NUMBER; else -> Tag.PHRASE
            }
            Word(
                id = f[0].lowercase().replace(' ', '_'),
                english = f[0],
                korean = f[1],
                level = 1,
                tag = tag,
                attr = f.getOrElse(3) { "" },
                pred = f.getOrElse(4) { "" },
                topic = topic,
            )
        }
        .toList()
    }

    val words: List<Word> = vocabulary() + listOf(
        Word("left", "Left", "왼쪽", 1, Cue.LEFT),
        Word("right", "Right", "오른쪽", 1, Cue.RIGHT),
        Word("stop", "Stop", "멈춰", 1, Cue.STOP),
        Word("go", "Go", "출발", 1, Cue.GO),
        Word("rain", "Rain", "비", 1, Cue.RAIN),
        Word("seatbelt", "Seatbelt", "안전벨트", 1, Cue.BELT),
        Word("back", "Back", "뒤로", 1, Cue.BACK),
        Word("are_we_there_yet", "Are we there yet?", "아직 멀었어?", 1, Cue.HOW_FAR),

        // ── 레벨 2: 짧은 표현 ────────────────────────────────────
        // 이야기와 강아지
        Word("little_puppy", "A little puppy", "작은 강아지", 2),
        Word("bright_moon", "The bright moon", "밝은 달", 2),
        Word("little_star", "A little star", "작은 별", 2),
        Word("soft_cloud", "A soft cloud", "폭신한 구름", 2),
        Word("look_up", "Look up!", "위를 봐", 2),
        Word("lets_fly", "Let's fly!", "날아가자", 2),
        Word("be_quiet", "Be quiet.", "조용히 해요", 2),
        Word("so_pretty", "So pretty!", "정말 예쁘다", 2),
        Word("yummy", "Yummy!", "맛있다", 2),
        Word("good_dog", "Good dog!", "착한 강아지", 2),
        Word("thank_you", "Thank you.", "고마워", 2),
        Word("good_night", "Good night.", "잘 자", 2),
        Word("see_you", "See you again!", "또 만나", 2),
        Word("wag_tail", "Wag your tail!", "꼬리를 흔들어", 2),
        Word("fluffy_puppy", "A fluffy puppy", "복슬복슬 강아지", 2),
        Word("sweet_dreams", "Sweet dreams.", "좋은 꿈 꿔", 2),
        // 인사와 대답
        Word("good_morning", "Good morning!", "좋은 아침", 2),
        Word("how_are_you", "How are you?", "잘 지냈어?", 2),
        Word("im_fine", "I'm fine.", "난 괜찮아", 2),
        Word("nice_to_meet", "Nice to meet you.", "만나서 반가워", 2),
        Word("say_hello", "Say hello!", "인사해", 2),
        Word("me_too", "Me too!", "나도", 2),
        Word("not_yet", "Not yet.", "아직이야", 2),
        Word("almost", "Almost!", "거의 다 왔어", 2),
        Word("all_done", "All done!", "다 했다", 2),
        Word("one_more_time", "One more time!", "한 번 더", 2),
        Word("good_job", "Good job!", "잘했어", 2),
        Word("high_five", "High five!", "손뼉 마주치기", 2),
        Word("i_love_you", "I love you.", "사랑해", 2),
        // 몸으로 하는 말
        Word("sit_down", "Sit down.", "앉아요", 2),
        Word("come_here", "Come here!", "이리 와", 2),
        Word("hold_on", "Hold on!", "꽉 잡아", 2),
        Word("watch_out", "Watch out!", "조심해", 2),
        Word("over_there", "Over there!", "저기야", 2),
        Word("clap_hands", "Clap your hands!", "손뼉을 쳐", 2),
        Word("close_eyes", "Close your eyes.", "눈을 감아", 2),
        Word("open_window", "Open the window.", "창문을 열어", 2),
        Word("lets_play", "Let's play!", "놀자", 2),
        Word("lets_sing", "Let's sing!", "노래하자", 2),
        Word("lets_count", "Let's count!", "세어 보자", 2),
        // 창밖과 느낌
        Word("red_car", "A red car", "빨간 자동차", 2),
        Word("blue_sky", "The blue sky", "파란 하늘", 2),
        Word("big_bus", "A big bus", "커다란 버스", 2),
        Word("sunny_day", "A sunny day", "맑은 날", 2),
        Word("rainy_day", "A rainy day", "비 오는 날", 2),
        Word("up_and_down", "Up and down", "위로 아래로", 2),
        Word("big_and_small", "Big and small", "크고 작은", 2),
        Word("whats_that", "What's that?", "저게 뭐야?", 2),
        Word("its_a_bird", "It's a bird!", "새다", 2),
        Word("im_hungry", "I'm hungry.", "배고파", 2),
        Word("im_sleepy", "I'm sleepy.", "졸려", 2),
        Word("im_happy", "I'm happy!", "행복해", 2),

        // 차 안과 길
        Word("p_in_the_car", "In the car", "차 안에서", 2),
        Word("p_red_light", "Red light", "빨간불", 2),
        Word("p_green_light", "Green light", "초록불", 2),
        Word("p_slow_down", "Slow down.", "천천히 가요", 2),
        Word("p_go_straight", "Go straight.", "똑바로 가요", 2),
        Word("p_almost_home", "Almost home", "집에 거의 다 왔어", 2),
        Word("p_a_long_tunnel", "A long tunnel", "긴 터널", 2),
        Word("p_a_bumpy_road", "A bumpy road", "울퉁불퉁한 길", 2),
        Word("p_a_big_truck", "A big truck", "큰 트럭", 2),
        Word("p_a_yellow_bus", "A yellow bus", "노란 버스", 2),
        Word("p_a_fast_train", "A fast train", "빠른 기차", 2),
        Word("p_next_stop", "Next stop", "다음 정류장", 2),
        Word("p_road_trip", "Road trip!", "자동차 여행", 2),
        Word("p_on_the_bridge", "On the bridge", "다리 위에서", 2),
        Word("p_this_way", "This way!", "이쪽이야", 2),
        Word("p_that_way", "That way!", "저쪽이야", 2),
        Word("p_right_here", "Right here", "바로 여기", 2),
        Word("p_park_here", "Park here.", "여기 세워요", 2),
        // 날씨와 하늘
        Word("p_it_s_sunny", "It's sunny.", "맑아요", 2),
        Word("p_it_s_cloudy", "It's cloudy.", "흐려요", 2),
        Word("p_it_s_windy", "It's windy.", "바람이 불어요", 2),
        Word("p_it_s_snowing", "It's snowing!", "눈이 와요", 2),
        Word("p_it_s_hot", "It's hot.", "더워요", 2),
        Word("p_it_s_cold", "It's cold.", "추워요", 2),
        Word("p_a_big_rainbow", "A big rainbow", "커다란 무지개", 2),
        Word("p_dark_clouds", "Dark clouds", "먹구름", 2),
        Word("p_a_full_moon", "A full moon", "보름달", 2),
        Word("p_a_shooting_star", "A shooting star", "별똥별", 2),
        Word("p_good_weather", "Good weather", "좋은 날씨", 2),
        // 먹을 때
        Word("p_i_m_thirsty", "I'm thirsty.", "목말라", 2),
        Word("p_i_m_full", "I'm full.", "배불러", 2),
        Word("p_more_please", "More, please.", "더 주세요", 2),
        Word("p_yes_please", "Yes, please.", "네, 주세요", 2),
        Word("p_no_thank_you", "No, thank you.", "아니요, 괜찮아요", 2),
        Word("p_snack_time", "Snack time!", "간식 시간", 2),
        Word("p_let_s_eat", "Let's eat!", "먹자", 2),
        Word("p_so_sweet", "So sweet!", "정말 달다", 2),
        Word("p_too_hot", "Too hot!", "너무 뜨거워", 2),
        Word("p_cold_milk", "Cold milk", "차가운 우유", 2),
        Word("p_fresh_fruit", "Fresh fruit", "신선한 과일", 2),
        Word("p_a_cup_of_water", "A cup of water", "물 한 컵", 2),
        // 마음과 예절
        Word("p_i_m_tired", "I'm tired.", "피곤해", 2),
        Word("p_i_m_sad", "I'm sad.", "슬퍼", 2),
        Word("p_i_m_scared", "I'm scared.", "무서워", 2),
        Word("p_i_m_okay", "I'm okay.", "난 괜찮아", 2),
        Word("p_i_m_excited", "I'm excited!", "신나", 2),
        Word("p_i_m_sorry", "I'm sorry.", "미안해", 2),
        Word("p_it_s_okay", "It's okay.", "괜찮아", 2),
        Word("p_don_t_worry", "Don't worry.", "걱정 마", 2),
        Word("p_cheer_up", "Cheer up!", "힘내", 2),
        Word("p_so_fun", "So fun!", "정말 재미있다", 2),
        Word("p_so_big", "So big!", "정말 크다", 2),
        Word("p_so_fast", "So fast!", "정말 빠르다", 2),
        Word("p_wow_amazing", "Wow, amazing!", "우와, 대단해", 2),
        Word("p_i_like_it", "I like it.", "마음에 들어", 2),
        Word("p_i_love_it", "I love it!", "정말 좋아", 2),
        Word("p_of_course", "Of course!", "물론이지", 2),
        Word("p_i_don_t_know", "I don't know.", "모르겠어", 2),
        Word("p_i_know", "I know!", "알아", 2),
        Word("p_i_did_it", "I did it!", "내가 해냈어", 2),
        Word("p_you_did_it", "You did it!", "네가 해냈어", 2),
        Word("p_well_done", "Well done!", "참 잘했어", 2),
        Word("p_try_again", "Try again.", "다시 해 봐", 2),
        Word("p_my_turn", "My turn!", "내 차례", 2),
        Word("p_your_turn", "Your turn!", "네 차례", 2),
        Word("p_let_s_share", "Let's share.", "같이 나누자", 2),
        Word("p_help_me_please", "Help me, please.", "도와주세요", 2),
        Word("p_excuse_me", "Excuse me.", "실례합니다", 2),
        Word("p_you_re_welcome", "You're welcome.", "천만에요", 2),
        Word("p_good_idea", "Good idea!", "좋은 생각이야", 2),
        Word("p_happy_birthday", "Happy birthday!", "생일 축하해", 2),
        Word("p_welcome", "Welcome!", "환영해", 2),
        Word("p_here_you_are", "Here you are.", "여기 있어", 2),
        // 같이 하는 말
        Word("p_follow_me", "Follow me!", "나를 따라와", 2),
        Word("p_wait_for_me", "Wait for me!", "나를 기다려", 2),
        Word("p_hurry_up", "Hurry up!", "서둘러", 2),
        Word("p_be_careful", "Be careful.", "조심해요", 2),
        Word("p_wake_up", "Wake up!", "일어나", 2),
        Word("p_bedtime", "Bedtime!", "잘 시간이야", 2),
        Word("p_time_to_go", "Time to go!", "갈 시간이야", 2),
        Word("p_let_s_read", "Let's read.", "책 읽자", 2),
        Word("p_let_s_dance", "Let's dance!", "춤추자", 2),
        Word("p_let_s_draw", "Let's draw.", "그림 그리자", 2),
        Word("p_let_s_hide", "Let's hide!", "숨자", 2),
        Word("p_hands_up", "Hands up!", "손 들어", 2),
        Word("p_jump_high", "Jump high!", "높이 뛰어", 2),
        Word("p_run_fast", "Run fast!", "빨리 달려", 2),
        Word("p_look_outside", "Look outside.", "밖을 봐", 2),
        Word("p_listen_carefully", "Listen carefully.", "잘 들어 봐", 2),
        Word("p_one_two_three", "One, two, three!", "하나, 둘, 셋", 2),
        Word("p_see_you_tomorrow", "See you tomorrow.", "내일 만나", 2),
        Word("p_come_in", "Come in.", "들어와", 2),
        Word("p_this_one", "This one!", "이거", 2),
        Word("p_that_one", "That one!", "저거", 2),
        Word("p_how_many", "How many?", "몇 개야?", 2),
        Word("p_who_is_it", "Who is it?", "누구야?", 2),
        Word("p_where_is_it", "Where is it?", "어디 있어?", 2),
        Word("p_brush_your_teeth", "Brush your teeth.", "이를 닦아", 2),
        Word("p_wash_your_hands", "Wash your hands.", "손을 씻어", 2),
        Word("p_put_on_your_shoes", "Put on your shoes.", "신발을 신어", 2),
        // 꾸며 주는 말
        Word("p_a_cute_kitten", "A cute kitten", "귀여운 아기 고양이", 2),
        Word("p_a_tall_tree", "A tall tree", "키 큰 나무", 2),
        Word("p_a_long_train", "A long train", "긴 기차", 2),
        Word("p_green_grass", "Green grass", "초록 풀", 2),
        Word("p_white_snow", "White snow", "하얀 눈", 2),
        Word("p_my_family", "My family", "우리 가족", 2),
        Word("p_my_best_friend", "My best friend", "가장 친한 친구", 2),
        Word("p_a_happy_day", "A happy day", "행복한 날", 2),
        Word("p_a_brave_knight", "A brave knight", "용감한 기사", 2),
        Word("p_a_tiny_ant", "A tiny ant", "아주 작은 개미", 2),
        Word("p_a_sleepy_bear", "A sleepy bear", "졸린 곰", 2),
        Word("p_a_shiny_star", "A shiny star", "반짝이는 별", 2),

        Word("turn_left", "Turn left.", "왼쪽으로 돌아요", 2, Cue.LEFT),
        Word("turn_right", "Turn right.", "오른쪽으로 돌아요", 2, Cue.RIGHT),
        Word("wait", "Wait a moment.", "잠깐 기다려요", 2, Cue.STOP),
        Word("lets_go", "Let's go!", "가자", 2, Cue.GO),
        Word("its_raining", "It's raining.", "비가 와요", 2, Cue.RAIN),
        Word("buckle_up", "Buckle up!", "벨트를 매요", 2, Cue.BELT),
        Word("go_back", "Go back.", "뒤로 가요", 2, Cue.BACK),

        // ── 레벨 3: 문장 ─────────────────────────────────────────
        // 이야기와 강아지
        Word("puppy_flying", "The puppy is flying.", "강아지가 날고 있어요", 3),
        Word("see_moon", "I can see the moon.", "달이 보여요", 3),
        Word("stars_shining", "The stars are shining.", "별이 빛나요", 3),
        Word("in_tunnel", "We are in the tunnel.", "우리는 터널 안에 있어요", 3),
        Word("wind_blowing", "The wind is blowing.", "바람이 불어요", 3),
        Word("what_see", "What can you see?", "뭐가 보여?", 3),
        Word("almost_there", "We are almost there.", "거의 다 왔어요", 3),
        Word("beautiful_night", "What a beautiful night!", "정말 아름다운 밤이야", 3),
        Word("found_dream", "I found my dream.", "내 꿈을 찾았어요", 3),
        Word("puppy_hungry", "The puppy is hungry.", "강아지가 배고파요", 3),
        Word("puppy_wags", "The puppy wags its tail.", "강아지가 꼬리를 흔들어요", 3),
        Word("puppy_sleeping", "The puppy is sleeping.", "강아지가 자고 있어요", 3),
        Word("puppy_happy", "The puppy is happy.", "강아지가 행복해요", 3),
        Word("treat_yummy", "The treat is yummy.", "간식이 맛있어요", 3),
        Word("thanks_treat", "Thank you for the treat.", "간식 고마워요", 3),
        Word("go_island", "Let's go to the island.", "섬으로 가자", 3),
        Word("flying_high", "We are flying high.", "우리는 높이 날고 있어요", 3),
        Word("clouds_soft", "The clouds are soft.", "구름이 폭신해요", 3),
        Word("moon_bright", "The moon is bright tonight.", "오늘 밤 달이 밝아요", 3),
        Word("hear_wind", "I can hear the wind.", "바람 소리가 들려요", 3),
        // 차 안과 창밖
        Word("sun_shining", "The sun is shining.", "해가 빛나요", 3),
        Word("see_red_car", "I see a red car.", "빨간 자동차가 보여요", 3),
        Word("where_going", "Where are we going?", "우리 어디 가?", 3),
        Word("going_home", "We are going home.", "우리는 집에 가요", 3),
        Word("bus_big", "The bus is big.", "버스가 커요", 3),
        Word("look_sky", "Look at the sky!", "하늘을 봐", 3),
        Word("see_bird", "Can you see the bird?", "새가 보여?", 3),
        Word("like_stars", "I like the stars.", "나는 별이 좋아요", 3),
        Word("beautiful_day", "It is a beautiful day.", "정말 좋은 날이에요", 3),
        Word("what_color", "What color is it?", "무슨 색이야?", 3),
        Word("count_three", "Count to three!", "셋까지 세어 봐", 3),
        Word("please_sit", "Please sit down.", "앉아 주세요", 3),
        Word("hold_hand", "Hold my hand.", "내 손을 잡아", 3),
        Word("i_am_ready", "I am ready!", "나는 준비됐어", 3),
        Word("are_you_ready", "Are you ready?", "준비됐어?", 3),

        // 차를 타고
        Word("s_we_are_in_the_car", "We are in the car.", "우리는 차 안에 있어요", 3),
        Word("s_the_light_is_red", "The light is red.", "신호등이 빨간불이에요", 3),
        Word("s_the_light_is_green", "The light is green.", "신호등이 초록불이에요", 3),
        Word("s_the_car_is_going_fast", "The car is going fast.", "차가 빨리 가요", 3),
        Word("s_the_car_is_slowing_down", "The car is slowing down.", "차가 천천히 가요", 3),
        Word("s_we_are_on_the_highway", "We are on the highway.", "우리는 고속도로 위에 있어요", 3),
        Word("s_we_are_crossing_the_bridge", "We are crossing the bridge.", "다리를 건너고 있어요", 3),
        Word("s_i_can_see_a_big_truck", "I can see a big truck.", "큰 트럭이 보여요", 3),
        Word("s_the_bus_is_yellow", "The bus is yellow.", "버스가 노란색이에요", 3),
        Word("s_look_at_that_airplane", "Look at that airplane!", "저 비행기를 봐", 3),
        Word("s_we_are_going_to_school", "We are going to school.", "우리는 학교에 가요", 3),
        Word("s_we_are_going_to_grandma_s_house", "We are going to Grandma's house.", "할머니 집에 가요", 3),
        Word("s_let_s_go_to_the_park", "Let's go to the park.", "공원에 가자", 3),
        Word("s_how_far_is_it", "How far is it?", "얼마나 멀어?", 3),
        Word("s_we_will_be_there_soon", "We will be there soon.", "곧 도착할 거예요", 3),
        Word("s_i_am_sitting_in_my_seat", "I am sitting in my seat.", "내 자리에 앉아 있어요", 3),
        Word("s_dad_is_driving_the_car", "Dad is driving the car.", "아빠가 운전해요", 3),
        Word("s_mom_is_driving_the_car", "Mom is driving the car.", "엄마가 운전해요", 3),
        Word("s_the_wipers_are_moving", "The wipers are moving.", "와이퍼가 움직여요", 3),
        Word("s_the_road_is_long", "The road is long.", "길이 길어요", 3),
        Word("s_there_is_a_tunnel_ahead", "There is a tunnel ahead.", "앞에 터널이 있어요", 3),
        Word("s_let_s_stop_at_the_rest_area", "Let's stop at the rest area.", "휴게소에 들르자", 3),
        // 날씨와 하늘
        Word("s_it_is_sunny_today", "It is sunny today.", "오늘은 맑아요", 3),
        Word("s_it_is_snowing_outside", "It is snowing outside.", "밖에 눈이 와요", 3),
        Word("s_the_sky_is_blue", "The sky is blue.", "하늘이 파래요", 3),
        Word("s_the_clouds_are_white", "The clouds are white.", "구름이 하얘요", 3),
        Word("s_i_can_see_a_rainbow", "I can see a rainbow.", "무지개가 보여요", 3),
        Word("s_the_moon_is_round", "The moon is round.", "달이 둥글어요", 3),
        Word("s_the_stars_are_far_away", "The stars are far away.", "별은 멀리 있어요", 3),
        Word("s_it_is_very_windy_today", "It is very windy today.", "오늘은 바람이 많이 불어요", 3),
        Word("s_the_leaves_are_falling", "The leaves are falling.", "나뭇잎이 떨어져요", 3),
        Word("s_spring_is_coming", "Spring is coming.", "봄이 오고 있어요", 3),
        // 먹고 지내는 말
        Word("s_i_want_some_water", "I want some water.", "물을 마시고 싶어요", 3),
        Word("s_i_like_apples", "I like apples.", "나는 사과를 좋아해요", 3),
        Word("s_this_is_yummy", "This is yummy.", "이거 맛있어요", 3),
        Word("s_can_i_have_some_more", "Can I have some more?", "더 먹어도 돼요?", 3),
        Word("s_i_am_very_hungry", "I am very hungry.", "배가 많이 고파요", 3),
        Word("s_let_s_have_lunch", "Let's have lunch.", "점심 먹자", 3),
        Word("s_i_brushed_my_teeth", "I brushed my teeth.", "이를 닦았어요", 3),
        Word("s_i_washed_my_hands", "I washed my hands.", "손을 씻었어요", 3),
        Word("s_it_is_time_for_bed", "It is time for bed.", "잘 시간이에요", 3),
        Word("s_it_is_time_to_go_home", "It is time to go home.", "집에 갈 시간이에요", 3),
        Word("s_i_put_on_my_shoes", "I put on my shoes.", "신발을 신었어요", 3),
        // 마음과 사람
        Word("s_i_am_happy_today", "I am happy today.", "오늘 나는 행복해요", 3),
        Word("s_i_am_a_little_tired", "I am a little tired.", "조금 피곤해요", 3),
        Word("s_i_love_my_family", "I love my family.", "나는 우리 가족을 사랑해요", 3),
        Word("s_you_are_my_best_friend", "You are my best friend.", "너는 내 가장 친한 친구야", 3),
        Word("s_i_miss_my_grandma", "I miss my grandma.", "할머니가 보고 싶어요", 3),
        Word("s_my_mom_is_kind", "My mom is kind.", "우리 엄마는 친절해요", 3),
        Word("s_my_dad_is_strong", "My dad is strong.", "우리 아빠는 힘이 세요", 3),
        Word("s_this_is_my_friend", "This is my friend.", "내 친구예요", 3),
        Word("s_what_is_your_name", "What is your name?", "이름이 뭐야?", 3),
        Word("s_how_old_are_you", "How old are you?", "몇 살이야?", 3),
        Word("s_can_you_help_me", "Can you help me?", "도와줄 수 있어?", 3),
        Word("s_i_can_do_it", "I can do it.", "나는 할 수 있어요", 3),
        Word("s_let_s_do_it_together", "Let's do it together.", "같이 해 보자", 3),
        Word("s_that_was_fun", "That was fun!", "재미있었어", 3),
        Word("s_i_had_a_good_day", "I had a good day.", "좋은 하루였어요", 3),
        Word("s_see_you_next_time", "See you next time.", "다음에 또 만나", 3),
        Word("s_i_am_not_scared", "I am not scared.", "나는 무섭지 않아요", 3),
        Word("s_it_is_my_turn", "It is my turn.", "내 차례예요", 3),
        Word("s_please_wait_for_me", "Please wait for me.", "나를 기다려 주세요", 3),
        Word("s_thank_you_very_much", "Thank you very much.", "정말 고마워요", 3),
        // 동물과 자연
        Word("s_the_bird_is_singing", "The bird is singing.", "새가 노래해요", 3),
        Word("s_the_dog_is_barking", "The dog is barking.", "개가 짖어요", 3),
        Word("s_the_cat_is_sleeping", "The cat is sleeping.", "고양이가 자요", 3),
        Word("s_the_rabbit_is_jumping", "The rabbit is jumping.", "토끼가 뛰어요", 3),
        Word("s_the_fish_is_swimming", "The fish is swimming.", "물고기가 헤엄쳐요", 3),
        Word("s_the_butterfly_is_flying", "The butterfly is flying.", "나비가 날아요", 3),
        Word("s_the_flower_is_pretty", "The flower is pretty.", "꽃이 예뻐요", 3),
        Word("s_the_tree_is_very_tall", "The tree is very tall.", "나무가 아주 커요", 3),
        Word("s_the_elephant_is_big", "The elephant is big.", "코끼리가 커요", 3),
        Word("s_the_mouse_is_small", "The mouse is small.", "쥐가 작아요", 3),
        Word("s_the_lion_is_strong", "The lion is strong.", "사자는 힘이 세요", 3),
        Word("s_the_turtle_is_slow", "The turtle is slow.", "거북이는 느려요", 3),
        Word("s_the_rabbit_is_fast", "The rabbit is fast.", "토끼는 빨라요", 3),
        Word("s_the_frog_jumps_high", "The frog jumps high.", "개구리가 높이 뛰어요", 3),
        Word("s_the_duck_says_quack", "The duck says quack.", "오리가 꽥꽥 해요", 3),
        Word("s_the_cow_says_moo", "The cow says moo.", "소가 음매 해요", 3),
        // 놀이와 물음
        Word("s_let_s_play_a_game", "Let's play a game.", "놀이를 하자", 3),
        Word("s_let_s_sing_a_song", "Let's sing a song.", "노래를 부르자", 3),
        Word("s_let_s_count_the_cars", "Let's count the cars.", "자동차를 세어 보자", 3),
        Word("s_can_you_find_a_red_car", "Can you find a red car?", "빨간 자동차를 찾을 수 있어?", 3),
        Word("s_i_found_it", "I found it!", "찾았다", 3),
        Word("s_how_many_stars_are_there", "How many stars are there?", "별이 몇 개 있어?", 3),
        Word("s_what_is_this", "What is this?", "이게 뭐야?", 3),
        Word("s_what_time_is_it", "What time is it?", "지금 몇 시야?", 3),
        Word("s_who_is_that", "Who is that?", "저 사람은 누구야?", 3),
        Word("s_where_is_the_puppy", "Where is the puppy?", "강아지는 어디 있어?", 3),
        Word("s_which_one_do_you_like", "Which one do you like?", "어느 것이 좋아?", 3),
        Word("s_i_like_this_one", "I like this one.", "나는 이게 좋아요", 3),
        Word("s_it_is_under_the_seat", "It is under the seat.", "좌석 아래에 있어요", 3),
        Word("s_it_is_next_to_me", "It is next to me.", "내 옆에 있어요", 3),
        Word("s_let_s_take_a_picture", "Let's take a picture.", "사진을 찍자", 3),
        Word("s_i_can_jump_very_high", "I can jump very high.", "아주 높이 뛸 수 있어요", 3),
        Word("s_i_can_run_fast", "I can run fast.", "빨리 달릴 수 있어요", 3),
        Word("s_i_can_swim", "I can swim.", "수영할 수 있어요", 3),
        Word("s_i_want_to_be_a_pilot", "I want to be a pilot.", "조종사가 되고 싶어요", 3),
        Word("s_let_s_go_camping", "Let's go camping.", "캠핑 가자", 3),
        Word("s_today_is_my_birthday", "Today is my birthday.", "오늘은 내 생일이에요", 3),
        Word("s_i_go_to_kindergarten", "I go to kindergarten.", "나는 유치원에 다녀요", 3),
        Word("s_i_read_a_book_every_night", "I read a book every night.", "매일 밤 책을 읽어요", 3),
        Word("s_let_s_clean_up", "Let's clean up.", "정리하자", 3),
        Word("s_good_night_moon", "Good night, moon.", "달아, 잘 자", 3),
        Word("s_see_you_in_my_dreams", "See you in my dreams.", "꿈에서 만나", 3),

        Word("turning_left", "We are turning left.", "왼쪽으로 돌고 있어요", 3, Cue.LEFT),
        Word("turning_right", "We are turning right.", "오른쪽으로 돌고 있어요", 3, Cue.RIGHT),
        Word("car_stopped", "The car stopped.", "차가 멈췄어요", 3, Cue.STOP),
        Word("here_we_go", "Here we go!", "출발한다", 3, Cue.GO),
        Word("raining_outside", "It is raining outside.", "밖에 비가 와요", 3, Cue.RAIN),
        Word("fasten_seatbelt", "Fasten your seatbelt.", "안전벨트를 매세요", 3, Cue.BELT),
        Word("going_back", "We are going back.", "뒤로 가고 있어요", 3, Cue.BACK),
    )

    private val byId = words.associateBy { it.id }

    init {
        check(byId.size == words.size) { "duplicate word id" }
    }

    /** 단어장의 말, 또는 익힌 낱말로 만든 문장([Sentences]). 없으면 null. */
    fun find(id: String): Word? = byId[id] ?: Sentences.decode(id)
}
