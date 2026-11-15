package com.example.madomireader.data.source.mock

import com.example.madomireader.domain.model.MangaStatus

/**
 * Dữ liệu mẫu dùng cho hai nguồn demo. Nội dung hội thoại được viết bằng
 * tiếng Anh / tiếng Trung để có thể kiểm thử luồng OCR → dịch ngay trên máy.
 */
object MockCatalog {

    data class MockSeed(
        val title: String,
        val author: String,
        val status: MangaStatus,
        val genres: List<String>,
        val description: String,
        val chapterCount: Int,
        val pagesPerChapter: Int,
        val dialogue: List<String>,
    )

    val genres: List<String> = listOf(
        "Hành động",
        "Phiêu lưu",
        "Học đường",
        "Hài hước",
        "Lãng mạn",
        "Kinh dị",
        "Fantasy",
        "Võ thuật",
        "Đời thường",
        "Bí ẩn",
        "Khoa học viễn tưởng",
        "Chính kịch",
    )

    val seeds: List<MockSeed> = listOf(
        MockSeed(
            title = "Solo Hunter: Thợ Săn Cấp E",
            author = "Jang Seong-rak",
            status = MangaStatus.ONGOING,
            genres = listOf("Hành động", "Fantasy", "Phiêu lưu"),
            description = "Hắn là thợ săn yếu nhất trong tất cả các thợ săn. Nhưng một hệ thống bí ẩn " +
                "xuất hiện giữa lúc sinh tử, và từ đó hắn bắt đầu leo lên từng bậc thang sức mạnh.",
            chapterCount = 32,
            pagesPerChapter = 6,
            dialogue = listOf(
                "You are the weakest hunter alive. Why do you keep going into these dungeons?",
                "Because if I stop now, everyone I promised to protect will die.",
                "This gate is rated D-rank. We should be fine if we stay together.",
                "Wait. Something is wrong. The mana density is rising too fast!",
                "I can't move... my legs won't listen to me anymore.",
                "A system window appeared in front of me. It said: you have been chosen.",
            ),
        ),
        MockSeed(
            title = "Kiếm Vực Truyền Kỳ",
            author = "Lâm Thanh Phong",
            status = MangaStatus.ONGOING,
            genres = listOf("Võ thuật", "Phiêu lưu", "Hành động"),
            description = "Thiếu niên bị phế võ công, bị cả gia tộc ruồng bỏ, tình cờ nhận được truyền thừa " +
                "của Kiếm Thần ngàn năm trước.",
            chapterCount = 48,
            pagesPerChapter = 5,
            dialogue = listOf(
                "你不过是个废物，也配站在这里？",
                "今日之辱，我必百倍奉还。",
                "这把剑，已经等了你一千年。",
                "师父，弟子愿以性命护此剑道。",
                "山门之前，谁敢与我一战？",
                "剑气纵横三万里，一剑光寒十九州。",
            ),
        ),
        MockSeed(
            title = "Học Viện Phép Thuật Aurora",
            author = "Mina Hoshino",
            status = MangaStatus.ONGOING,
            genres = listOf("Fantasy", "Học đường", "Hài hước"),
            description = "Cô gái nông thôn trúng tuyển vào học viện phép thuật danh giá nhất lục địa, " +
                "nhưng phép thuật của cô luôn nổ tung theo cách không ai ngờ.",
            chapterCount = 24,
            pagesPerChapter = 5,
            dialogue = listOf(
                "Welcome to Aurora Academy! Please keep your wands pointed away from the library.",
                "My spell exploded again. Is that... normal for a first year?",
                "Normal? The last person who did that became an archmage. Or a crater.",
                "You should join the dueling club. We could use someone unpredictable.",
                "I came here to study, not to blow up the east wing!",
                "Too late for that. The east wing is already gone.",
            ),
        ),
        MockSeed(
            title = "Đêm Của Quỷ Đỏ",
            author = "Trần Hạo Nhiên",
            status = MangaStatus.COMPLETED,
            genres = listOf("Kinh dị", "Bí ẩn", "Chính kịch"),
            description = "Mười hai người lạ tỉnh dậy trong một ngôi làng không có đường ra. " +
                "Mỗi đêm, một người biến mất.",
            chapterCount = 18,
            pagesPerChapter = 4,
            dialogue = listOf(
                "Nobody leaves this village after sunset. Nobody.",
                "The door was locked from the inside. So who took her?",
                "I heard footsteps on the roof. There is nothing above the roof!",
                "Don't look at the red moon. Whatever you do, don't look at it.",
                "It knows my name. How does it know my name?",
                "We have one hour until sunrise. Run.",
            ),
        ),
        MockSeed(
            title = "Tiệm Cà Phê Của Nữ Phản Diện",
            author = "Yoo Seo-a",
            status = MangaStatus.ONGOING,
            genres = listOf("Lãng mạn", "Đời thường", "Hài hước"),
            description = "Cô tỉnh dậy trong cuốn tiểu thuyết mình từng đọc, với thân phận nữ phản diện " +
                "sắp bị xử tử. Cách duy nhất để sống: mở một tiệm cà phê thật ngon.",
            chapterCount = 28,
            pagesPerChapter = 5,
            dialogue = listOf(
                "Lady Elliana, the crown prince requests an audience with you.",
                "Tell him I am busy roasting beans. Very busy. Extremely busy.",
                "This coffee is... I have never tasted anything like this in my life.",
                "Then you should come back tomorrow. And bring your friends.",
                "The original story said I would die in chapter three. We are in chapter twelve.",
                "Perhaps the story is not the only thing that can be rewritten.",
            ),
        ),
        MockSeed(
            title = "Trạm Cứu Hộ Vũ Trụ Số 7",
            author = "Kenji Aoyama",
            status = MangaStatus.ONGOING,
            genres = listOf("Khoa học viễn tưởng", "Hài hước", "Phiêu lưu"),
            description = "Trạm không gian tọa lạc ở rìa thiên hà, nơi những kẻ lạc đường nhất vũ trụ " +
                "tình cờ ghé qua xin... một chỗ ngủ.",
            chapterCount = 20,
            pagesPerChapter = 5,
            dialogue = listOf(
                "Station seven, we have an unidentified craft requesting docking.",
                "Grant it. If it wanted to attack, it would not have knocked.",
                "Captain, there is a talking octopus in the airlock. He says he is the ambassador.",
                "Of course he is. Prepare the good tea and hide the emergency rations.",
                "The engine is dead, the fuel is gone, and I still have to file a report.",
                "Reports are the only thing in this galaxy that never break down.",
            ),
        ),
        MockSeed(
            title = "Trở Lại Ngày Thanh Xuân",
            author = "Park Ha-eun",
            status = MangaStatus.COMPLETED,
            genres = listOf("Học đường", "Lãng mạn", "Chính kịch"),
            description = "Người đàn ông 40 tuổi thất bại trong công việc bỗng tỉnh dậy vào đúng " +
                "ngày đầu tiên của năm lớp 11.",
            chapterCount = 22,
            pagesPerChapter = 4,
            dialogue = listOf(
                "Why is the classroom clock showing 2009?",
                "Hey, new kid. Are you going to stand in the hallway all day?",
                "I know exactly what happens next. This time, I will not run away.",
                "You are smiling for no reason. Are you sick?",
                "I am fine. Better than I have been in twenty years.",
                "Then prove it. Join the club before the deadline.",
            ),
        ),
        MockSeed(
            title = "Thần Thú Sư Bất Đắc Dĩ",
            author = "Nguyễn Bảo Khang",
            status = MangaStatus.ONGOING,
            genres = listOf("Fantasy", "Phiêu lưu", "Hành động"),
            description = "Cậu bé chăn cừu vô tình cứu một con rồng non. Từ đó, cả thế giới " +
                "coi cậu là kẻ nguy hiểm nhất lục địa.",
            chapterCount = 36,
            pagesPerChapter = 6,
            dialogue = listOf(
                "You saved a dragon hatchling. Do you understand what that means?",
                "It means I have a very hungry pet, and no idea how to feed it.",
                "It means every kingdom in the world is now watching you.",
                "Then let them watch. I still have to shear the sheep before sunset.",
                "The dragon just burned the sheep pen. Again.",
                "We will call that a training exercise.",
            ),
        ),
        MockSeed(
            title = "Bản Giao Hưởng Của Biển",
            author = "Elena Marchetti",
            status = MangaStatus.PAUSED,
            genres = listOf("Chính kịch", "Lãng mạn", "Đời thường"),
            description = "Nghệ sĩ violin mất thính lực sau tai nạn, trở về quê chài lưới và gặp " +
                "một cô gái có thể nghe được nhịp sóng.",
            chapterCount = 14,
            pagesPerChapter = 4,
            dialogue = listOf(
                "You cannot hear the sea anymore, can you?",
                "I hear the shape of it. That is enough for now.",
                "Then I will play for you. Every evening, until you can hear it again.",
                "That is a very heavy promise for someone you just met.",
                "I am not afraid of heavy things. I am afraid of silence.",
                "Then let us fill the silence together.",
            ),
        ),
        MockSeed(
            title = "Ma Thư Đường Phố",
            author = "Zhou Mu",
            status = MangaStatus.ONGOING,
            genres = listOf("Bí ẩn", "Kinh dị", "Võ thuật"),
            description = "Thư viện gia phả ở khu phố cũ chứa những cuốn sách kể về cái chết của " +
                "người đọc chúng.",
            chapterCount = 26,
            pagesPerChapter = 5,
            dialogue = listOf(
                "These books write themselves at night. Do you know why?",
                "Because someone in this city is about to die.",
                "Then we should find the reader before the last page.",
                "The last page is always blank. That is how you know it is close.",
                "The lamp just went out. Do not move. Do not breathe.",
                "I found the name. It is yours.",
            ),
        ),
        MockSeed(
            title = "Hợp Đồng Nuôi Rồng Con",
            author = "Lê Minh Thư",
            status = MangaStatus.ONGOING,
            genres = listOf("Hài hước", "Đời thường", "Fantasy"),
            description = "Một kế toán bình thường ký nhầm hợp đồng chăm sóc rồng trong ba trăm năm. " +
                "Không có điều khoản hủy.",
            chapterCount = 30,
            pagesPerChapter = 5,
            dialogue = listOf(
                "Clause fourteen: the caretaker must never abandon the dragon.",
                "I did not read clause fourteen. Nobody reads clause fourteen!",
                "The dragon is awake. The dragon is hungry. The dragon is on fire.",
                "Please stop setting the curtains on fire. Please.",
                "It only does that when it likes you.",
                "That is the worst way anyone has ever shown affection.",
            ),
        ),
        MockSeed(
            title = "Thành Phố Không Ngủ",
            author = "Han Ji-woo",
            status = MangaStatus.ONGOING,
            genres = listOf("Hành động", "Bí ẩn", "Chính kịch"),
            description = "Thám tử mất trí nhớ truy tìm kẻ đã xoá ký ức của mình, trong một thành phố " +
                "chưa từng thấy ánh mặt trời trong mười năm.",
            chapterCount = 34,
            pagesPerChapter = 6,
            dialogue = listOf(
                "Your file says you have no past. Do you believe it?",
                "I believe the person who wrote that file is still alive.",
                "Then we have a place to start. The archive on level nine.",
                "Level nine is sealed. Nobody has been down there since the blackout.",
                "Someone has. Look at the dust. It was disturbed this morning.",
                "Then we are not the first ones looking.",
            ),
        ),
        MockSeed(
            title = "Cô Gái Vẽ Truyện Trên Trời",
            author = "Aoi Kurihara",
            status = MangaStatus.ONGOING,
            genres = listOf("Đời thường", "Lãng mạn", "Học đường"),
            description = "Mỗi bức vẽ cô thả lên bầu trời đều trở thành một sự kiện có thật. " +
                "Điều đó rất vui, cho đến khi cô vẽ mất một người.",
            chapterCount = 16,
            pagesPerChapter = 4,
            dialogue = listOf(
                "The balloon you released this morning — it turned into a real cloud.",
                "That is impossible. I only drew a cloud because I was bored.",
                "Then stop drawing. Please. Before something goes wrong.",
                "I drew a friend once. He is standing next to you right now.",
                "There is nobody standing next to me.",
                "Exactly. Not yet.",
            ),
        ),
        MockSeed(
            title = "Vị Vua Cuối Cùng Của Rừng Đen",
            author = "Fernando Silva",
            status = MangaStatus.CANCELLED,
            genres = listOf("Fantasy", "Chính kịch", "Phiêu lưu"),
            description = "Vị vua cuối cùng của khu rừng bị nguyền rủa phải chọn giữa việc cứu " +
                "vương quốc của mình hay cứu người duy nhất còn tin vào ông.",
            chapterCount = 12,
            pagesPerChapter = 4,
            dialogue = listOf(
                "The forest is dying, my king. The roots no longer remember our names.",
                "Then I will remind them. Even if it costs me the crown.",
                "There is one person left who still believes you can save this place.",
                "Then let us not disappoint her.",
                "My king, the black mist is at the gate.",
                "Open it. I will walk out to meet it.",
            ),
        ),
        MockSeed(
            title = "Tổ Đội Bếp Trưởng Siêu Cấp",
            author = "Trịnh Gia Bảo",
            status = MangaStatus.ONGOING,
            genres = listOf("Hài hước", "Đời thường", "Hành động"),
            description = "Đội đặc nhiệm tinh nhuệ nhất quốc gia bị điều về mở một chuỗi quán ăn " +
                "đêm để cải trang.",
            chapterCount = 18,
            pagesPerChapter = 4,
            dialogue = listOf(
                "Our cover is a noodle shop. Please take this seriously.",
                "I take noodles very seriously. This broth has been simmering for nine hours.",
                "There is a suspicious van parked outside for the third night in a row.",
                "Then we serve them the special. Nobody comes back after the special.",
                "Sir, the special is just extra chili.",
                "Exactly. Nobody comes back.",
            ),
        ),
        MockSeed(
            title = "Nữ Hoàng Cờ Vây",
            author = "Kim Da-in",
            status = MangaStatus.COMPLETED,
            genres = listOf("Chính kịch", "Học đường", "Đời thường"),
            description = "Cô gái đến từ thị trấn nhỏ đánh bại nhà vô địch quốc gia trong một ván cờ " +
                "được livestream cho cả nước xem.",
            chapterCount = 20,
            pagesPerChapter = 4,
            dialogue = listOf(
                "You resigned on move ninety-seven. Why?",
                "Because I saw the shape of the board, and it was already over.",
                "Nobody resigns against the national champion. Nobody.",
                "Then today somebody did.",
                "Do you want to play again? I still have not shown you my real opening.",
                "Set up the board. I have time for one more.",
            ),
        ),
    )
}
