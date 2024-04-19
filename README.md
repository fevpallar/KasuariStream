<h1><img src="https://github.com/fevpallar/KasuariStream/assets/17115595/740c5083-b4e6-4ee4-9635-aea981cbae08" width="38" /> Kasuari Stream </h1>>
Previously, I was thinking about developing some video stream/call based app, just to kill time.
But after some research, I found that developing video streaming app from scratch is actually...hard.

Most video streaming/ video chat apps nowdays utilize some **SDK for voice & video call** to quickly develop 
their video streaming apps, like _Agora SDK_, _Stream SDK_ , _sendBird_, etc. Problem is, **they're not free**. Subscription needed, Or free with basic features

Another alternative is to use **WebRTC**. It's free. However, with it, I likely
don't have direct access over low-level process/resources (such as _multiplexing, buffers/bytes modification, transmission, encoding-decoding, threading structure_, and many others parameters). In other words, can only develop a plain stream app. Meanwhile, Complex processing and customization need us to have such control over many attributes at/after the moment(s) the frames get processed in memory/cpu , For ex. To develop detection, segmentation, transformation, etc.

Luckily `Java` API has provided all those low-level/hardware parameters. But, Developing the app itself is another story. As, things are indeed complicated.

*Currently, I'm still figuring out how to properly manage the memory & transmission in my code as streaming needs everything be processed fast through network. Still around this :

![TO github](https://github.com/fevpallar/KasuariStream/assets/17115595/549ef410-e321-454f-8e27-08120a34b339)






