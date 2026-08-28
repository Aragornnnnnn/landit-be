-- 고정 질문 음원 URL을 저장하고 캐릭터 TTS 음성을 Aura 2로 통일한다.
ALTER TABLE scenario_question_language_variant
    ADD COLUMN audio_url VARCHAR(1000);

-- LAN-351 manifest: 2e084d63e194f984f0160341889d3df7e610b9de99f8dc528ee3f95211874509.json
UPDATE scenario_question_language_variant
SET audio_url =
    CASE scenario_question_id
        WHEN 1 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/1/4d8422c99edfb8fe16c06981ec87eac5ec99b727fd807ed70e5f18317f52878c.mp3'
        WHEN 2 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/2/496e2eb4167859d1cb072627ea0500baa3c0a3c2479c2f5f194560c0745e0ac2.mp3'
        WHEN 3 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/3/3de8a97d156b5db47c0bc9ecf2c519c24c8013c9622d0382a50f451171ad9a7e.mp3'
        WHEN 4 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/4/16368a8c2a6e9c86b12d6575702ab9f209d9ef18e37de6324d3ce263a0c2c460.mp3'
        WHEN 5 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/5/6c74bbd8595e6ce3124ebac35632a15fb630cbc29681b2dfd83a698a4b2869cf.mp3'
        WHEN 6 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/6/43850e4f9bb36c69f144bd12ca5b8e64025e682e99ede699c63ce2174a17c932.mp3'
        WHEN 7 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/7/27ad1e592a6585a814704601a2513ed005b6ab8de641926099678b0306d7a96e.mp3'
        WHEN 8 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/8/188399e70aba9ac5e2a0df4a65712c2d52fad2734d4deae3a1e7494ccd990dfb.mp3'
        WHEN 9 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/9/f4654701de8c9a99a4465c3ebcbb7288f5e5123c96b56ad7ab0a9405fe4ddbb1.mp3'
        WHEN 10 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/10/6be75fcd53bd6f6e6f95dda51ddea8bca1ec629141c908d943b9eb3443b784af.mp3'
        WHEN 11 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/11/ac4abcad4f418a9a073fa08b91ebdf1880e677783bd04b141f2974519000240c.mp3'
        WHEN 12 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/12/489869a94456b72e7bc504906627289826f1044c27cf46fdd942b6bcf7fb947a.mp3'
        WHEN 13 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/13/6fa0c678f69adc5e355ec9582c594e6a6e3ab1beeda17e33a7fe3f4e5db2336a.mp3'
        WHEN 14 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/14/c794267a8f51058f35fc3bd3205c5a64982a60654aaadc67d118f8f1a4129121.mp3'
        WHEN 15 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/15/d86c934dcace4f6d3792789fa637970ebc7a6d6141115f9808c7e658782e610d.mp3'
        WHEN 16 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/16/825bffd9a13327f801194b36b604a505ba1aba77166e03087aeab180fbf09a7e.mp3'
        WHEN 17 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/17/2fd0d9e2328570ddc6d9c87e3e15c5971033d6d881dea76fc5f6c359b26c74b9.mp3'
        WHEN 18 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/18/46e7a3d209771246cea1166004e1fc3d888f08866dc7efe7e39b403d68a1c0bf.mp3'
        WHEN 19 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/19/2714b8b806f0ef719d33969eeb40e7b50264b92c1b7eab74c86911df6c6ca428.mp3'
        WHEN 20 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/20/8fc70f0aef8c7b9c4ddf6e39a0817f0ca0405937b4359bb9d59ba74170fcc651.mp3'
        WHEN 21 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/21/a5e47a4e3af632ccf88683bfb32b02192ed3007d739b739c3f00e1c8db999551.mp3'
        WHEN 22 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/22/04ca3e77fb6eea225efe2e76e611b78af021dee225c18f3cd1fc654fcfce1c7e.mp3'
        WHEN 23 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/23/b8a9b7775a9b173918263d44270de2823a35ab1d85a4fe3b661fe3b1a4ee3374.mp3'
        WHEN 24 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/24/7f48f67240753d3126f5d361d218c842d5c429c000c080147881851f9ec0df30.mp3'
        WHEN 25 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/25/138cc6a9510a8c5b393d73886c5e15373abf319fa0524f4ad8c6aa5e8bfd1ca6.mp3'
        WHEN 26 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/26/30613bdabafa91c4497a4f722139e11a30e3686c0e5ca11f15def229dde24641.mp3'
        WHEN 27 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/27/5a3388c4d201a855dd3b589ac748d3c7a605a746421c8b8ef758950418d5f6ac.mp3'
        WHEN 28 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/28/c7d9baafd0a5730acee13741549f0f41916c1c398a2a68669ea950b229db8e6d.mp3'
        WHEN 29 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/29/f0503d881fc5edc1906683a40b3160d4f39e88630ab7343e916d6dd79776f754.mp3'
        WHEN 30 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/30/550b13c4e4e1aa6050af0e17f0b2f094e4836755917f243988d7e7b7a37c68f9.mp3'
        WHEN 31 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/31/5f5ad25aae049eac15a52aa1d1a615b81e2e2269220fdac1582afdd607908580.mp3'
        WHEN 32 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/32/6c88d0325f5ab1bece82686a6b25cef2473f407985299fa84395a01551d470f7.mp3'
        WHEN 33 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/33/1e19e829dabf1a705a66c0a34c55f47a5d7a7947c1696a769f9f73f41e119e79.mp3'
        WHEN 34 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/34/e8d8cd0e4c167ac8af4563ea949202cbdff850990bb0b51bfb039170b434b71d.mp3'
        WHEN 35 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/35/a8777ea2d6543a832b9b9d865a825448b8d8d9b93379939b934662c3cb0631ca.mp3'
        WHEN 36 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/36/017d3166b72693cae917ff2cacfeb014f1b40e84f915026680c1d49f12034445.mp3'
        WHEN 37 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/37/bac220f8ea83c866574603c8a30908ca53e85c028f6b377621da00b1ceb47f4a.mp3'
        WHEN 38 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/38/1c66b9716a0d095640ab242db628ee7131e96f1f54dd6f4915436e94bb328182.mp3'
        WHEN 39 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/39/de1f5aa620fd50ca6c7ff9c5dc8728586f4cb79106a71590e00f24d210f7393a.mp3'
        WHEN 40 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/40/872afaaab4c18f64f43f42d1a27a38e4e1a0e55d91034e62897bf03b69a10c7f.mp3'
        WHEN 41 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/41/cbd12dc040e747f4ec77acc89d954639849167b283f3a436b131a40dc3355c0d.mp3'
        WHEN 42 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/42/35f06d849ded94979e6772a855799b8e0b65b180cc185ce4c9990de8f8a7e201.mp3'
        WHEN 43 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/43/3242222c8637d3a382a1183c6cff6f93c91962e5b1237700325889156aa000e0.mp3'
        WHEN 44 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/44/9063258b9958a7c789f33334c82d559d833adaecb054320998fe3ea3b47515a3.mp3'
        WHEN 45 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/45/8b65b3b62a091e43fe901a33b93070ac8f0a7a0d227b5e4ee2c19a576e230a80.mp3'
        WHEN 46 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/46/5b84b518cb709afee57f2e868744127952f188cd8f2cacb6d91c21d857e887ef.mp3'
        WHEN 47 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/47/f881b76f56bc86d3e51dd465039affcf30eda6515925e404bc9e618916bb2bfb.mp3'
        WHEN 48 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/48/d4406d31c3eb354ff8494731265d40d6e4249d592b5ac7c2042efda5ef43a7a2.mp3'
        WHEN 49 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/49/804fd18b5eb1151a1cca2c65a46ec75db90d501b823084a631d0c08ee7b0625d.mp3'
        WHEN 50 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/50/aa6dd0198c7eaef3ce10bd93aad77841882dc53e7ef8b5942035a1b9165c49a7.mp3'
        WHEN 51 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/51/41a4938951cc6fedf7e556d52adcf87218ce73258000c14579b6c36239def9d7.mp3'
        WHEN 52 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/52/163721b2884e19fca75f3542549fe03db82339aa4abf5782d357e47d5b74bf3a.mp3'
        WHEN 53 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/53/b432f85d85b6f05920405939dc2387d6245cb25e82abe96a563705b3eddba7d4.mp3'
        WHEN 54 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/54/919dc493c9e89dbd831fc3d622584339a978fcf7d267052bf8ed7832a00fda17.mp3'
        WHEN 55 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/55/c2ac10cc4a35e8210eb1e65f609323098efe09653dd7696801451951a374e29c.mp3'
        WHEN 56 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/56/f64c57b6c30e447f4b72de2cf688571a6d480262e85adeff134c56ffe2f2a7ea.mp3'
        WHEN 57 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/57/35f45d9c0ca91fd80dc087bb9afbc156cef2aebebca2b6c79c5abdcced1020df.mp3'
        WHEN 58 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/58/48b97612278085f7cf2f3ceaade076caa009a9143f83e979eda32f82ea6e6b54.mp3'
        WHEN 59 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/59/33a9c5a6ce0e2c30ea6ae1d0d3ca4c428189a7751aa358cc1d905fb3990027e3.mp3'
        WHEN 60 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/60/23805f4260e3e9e89828b68a5ca3c4fcacca9378b934bea90f93fd13b55406bd.mp3'
        WHEN 61 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/61/f90ad27cc66c7abcf9b6593cb48cd4627f67c50ce64d640b259ee47bf2f12010.mp3'
        WHEN 62 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/62/24746c7a447f44602aac2c9421b64a17b059ad3c4cc7ec5ea9a1df5fabec8963.mp3'
        WHEN 63 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/63/0d97b910a8010a77a9272a820f1f16b1a3fc948243c85a42d1eea5d6aedc96bf.mp3'
        WHEN 64 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/64/225d9de727e61e95f2878ea57a9013fb0cb2de6acd3799c0fe8a1e05d2bc3bcb.mp3'
        WHEN 65 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/65/3e4d7ec0c64f82da5a4fd5026f9a1e2961cc83f7a724ee8bcbd11ced61d4d126.mp3'
        WHEN 66 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/66/8b971d796c5285e464c13c7d82bed5257273ebf60a013aa0fd3261b7e213c200.mp3'
        WHEN 67 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/67/1a57a2554707f2a539b0bc94f5cdb805e6bcc5d08f89d7e8cdefc0ba286bdcaa.mp3'
        WHEN 68 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/68/432977bd13ec35ec5bd1d8588c18ec5712854266d8986bf8f9abad2069514ea0.mp3'
        WHEN 69 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/69/6a837e25a3d7599f3f1fe183bd82544b5416b2407949dfb4ddceeeb8d75d8fad.mp3'
        WHEN 70 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/70/783cdaf869f3b1883643588052f3ec39e822824e5cabe596ce8bd1821c33befb.mp3'
        WHEN 71 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/71/ea1d4c033426d0189df1a0668b3cf2f1831ec79edfbf2dce2956e8be92565a99.mp3'
        WHEN 72 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/72/d10bc1c6b5ad88eb52d7ce4e96da6e1cb76ecf7c1dcb6565e4422305bd892d36.mp3'
        WHEN 73 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/73/3ed7e03649505e767cb49d602efa37dce0e981b19a3b43ca5ba78a57bbbcb75b.mp3'
        WHEN 74 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/74/0b64b3c3c3e783857142a94e9fbd397b4af6f165e790f6fcb7d8bd454fd90c38.mp3'
        WHEN 75 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/75/c0a67d7287b3626b8154ea7d612156721eae16b94dd76022f4f9be38c7517be6.mp3'
        WHEN 76 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/76/edd897c7d8829f78f4d8eaa7a22a45697c01b22052f6f05aced10cc35e6e0cb9.mp3'
        WHEN 77 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/77/6cbe1f70ab7ac8b8293e1211837e1798c088c029b8b2dd04d9ed5cbb2f586aaf.mp3'
        WHEN 78 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/78/baf5ba074932ddcd1b610aa6511cf67a5244552a6327d0d0b6e92be1fd34f4db.mp3'
        WHEN 79 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/79/581ddb9438a7afa75cc276d47fac3f5e5d8707a325af516e93611c5dca9646de.mp3'
        WHEN 80 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/80/744a4bac9f34d6492c88df998c7eba67eec453a50172f2e925aececeacb8b414.mp3'
        WHEN 81 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/81/64bd725c302574665f3966a40ba52539a8ed50f3350886ddc96dc3ad8743afea.mp3'
        WHEN 82 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/82/53402d7e59b57d3ee8615ae6b878ed969e0fd127b598b56ce301f3c0cdb662e1.mp3'
        WHEN 83 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/83/d64dfc141781d25c9969bdf257e803b4d57718961238a0ab0a8d3e5fb7a715fd.mp3'
        WHEN 84 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/84/7b87aefcd726ef6c9bbaf53a3cef4086ac39e264a68384d8be501d04da49003a.mp3'
        WHEN 85 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/85/be49bd3d60f94d60506bc1b2041df84b4508ea863b09c868fd4cccda1d7c6973.mp3'
        WHEN 86 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/86/78cb652eab037dbc008f23fc607e0421c05aba133421776aa41e31ca82b648b9.mp3'
        WHEN 87 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/87/5abaa23d75992154fbcbc97eb953ab138abf590831162ee295b90876d92de255.mp3'
        WHEN 88 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/88/eceeda264d437367ec4fb9a4a19eada6ccf0c95d2df8e976ccb35bee90a01e7d.mp3'
        WHEN 89 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/89/a3bc3a5a18279fc5d61fa27961bede08262c724ade3f76651cedd6e43dc7aa59.mp3'
        WHEN 90 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/90/8af22e6216e9ffb8961405177c4c1810e12e150703730af4b47bd2c8527680c8.mp3'
        WHEN 91 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/91/8f3089059ab0dbeef65b5f4d9db128c8ea072667c604de6ecb4a5e2072157921.mp3'
        WHEN 92 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/92/d73f5eb02ec99169d594ee9561b280dabb2a29c7bc268ab36835219eb6ec43e5.mp3'
        WHEN 93 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/93/193b60fc1d3d1a4df674958cd366db9bdc88b4258edec5080d8de2aefcb056ee.mp3'
        WHEN 94 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/94/090d7eec63fdf63ec72d19e573d24b81ee4851426f14ba17abe70e76fee35a63.mp3'
        WHEN 95 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/95/7a2d5e61a21e74d3d541c252e34c6a820ce1ce916c2e004376da1b15130706ea.mp3'
        WHEN 96 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/96/1f063f3af129982ebddf6727248a6eb29649557391b9f99604629c18368e16d1.mp3'
        WHEN 97 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/97/1220929d5a3fc057fe38b095031d1ba06a479b27e01216759d3f155cabdec61d.mp3'
        WHEN 98 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/98/29f69f70108aca92349de8706cf89d5909721474a8aa00f869658f30bac233e9.mp3'
        WHEN 99 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/99/e8aa2ec238217ae1136bbab8b40aa82c38aac9217f506578a2b79910b0554598.mp3'
        WHEN 100 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/100/7421dfc042854df7aa42838e2ec83e1f538cbadfcd8b7b148fb05314b49091bc.mp3'
        WHEN 101 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/101/dbfabda44cd645ba52cf4d469c43c26e392433229175047133af8341e6d0002b.mp3'
        WHEN 102 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/102/1eb8c8b173749e98e4540fa7b7ae149572ae1c82191732d0d2b3b79b33c21af4.mp3'
        WHEN 103 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/103/efae3f759a147202c9c03b46623b3f66713168cef3771aaa6fa222ffe48ad726.mp3'
        WHEN 104 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/104/4e2be8d33abf1ce0f46d120b86806a0a84fab31748b0388a9b60c4d2062adaf5.mp3'
        WHEN 105 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/105/4d0ed5d51621e539c8a2cd9bd8383bca442a495a2f9054f160b4cd1d184cb5a2.mp3'
        WHEN 106 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/106/bb8eb673d910ecc4a8c5e32157ed172b848c091d74947e680d915920373a8c48.mp3'
        WHEN 107 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/107/e40b60f485f5514a5a5f8c1ec82e7f6544d449193e7b1d756f28cbc01da9a6fe.mp3'
        WHEN 108 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/108/e337961cb542394dd614b69c41eb0206a60831513dc860a49b5cc3ea53fa8a2a.mp3'
        WHEN 109 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/109/9d6cd91623578081802a6a10ba049430db16c8c148974cd95b78d3b807ed5b3e.mp3'
        WHEN 110 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/110/86b5a259e557e1a94119c36d121ae2d4ab5256eddaa7a2f0a6c1e7e517c388fa.mp3'
        WHEN 111 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/111/4e10cb3a2c91e1b4109e5da469eabd9159c7068caaacfd4ea739d96d25464a10.mp3'
        WHEN 112 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/112/1fd2265bb6663f2f7a80ba6ab26dd262170b2d120ee2745ec021ff1bf1704674.mp3'
        WHEN 113 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/113/c49e13f83f2099e81d5c32921a923164336d02ccc1eaf14e92d1fc9402dcc06b.mp3'
        WHEN 114 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/114/983da8607713303b6efa32279aee4fcbb7f54bab6b8b4014ea55b89895b7ec71.mp3'
        WHEN 115 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/115/b6960427e478f843dfad6e8d80154e7cb8b1da6848fe6e66d5635fac19d5ef30.mp3'
        WHEN 116 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/116/8c777cf2cd0d0e48daf98ade346926486b326defcbca90a58744754f97e0d8a0.mp3'
        WHEN 117 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/117/62b78c4e8ca2a7ee82a14ba4cbc5efd417caf69120b4b11550e40af7480cabed.mp3'
        WHEN 118 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/118/246661329c0f0a71bf8cb1aa52b5e62a77c1044d77e2b40f3029a7ca465f6222.mp3'
        WHEN 119 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/119/02d81112cad4830ea80f78372cd12e18bd2087e4015c365f97784273231c640e.mp3'
        WHEN 120 THEN 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/120/243f1571787cc506e750e7dbfe0a57b4b90fb9815288f376ee4ec5ce3d9ded12.mp3'
    END
WHERE scenario_question_id BETWEEN 1 AND 120;

ALTER TABLE scenario_question_language_variant
    ALTER COLUMN audio_url SET NOT NULL;

UPDATE tts_voice
SET model = 'deepgram/aura-2',
    provider_voice_id = 'aura-2-luna-en',
    description = '미국 영어 여성 음성',
    accent_locale = 'EN_US',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE tts_voice
SET model = 'deepgram/aura-2',
    provider_voice_id = 'aura-2-hyperion-en',
    description = '호주 영어 남성 음성',
    accent_locale = 'EN_AU',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

UPDATE tts_voice
SET model = 'deepgram/aura-2',
    provider_voice_id = 'aura-2-draco-en',
    description = '영국 영어 굵은 남성 음성',
    accent_locale = 'EN_GB',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;
