package oopexam.exambotoop;


import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class MyTelegramBot extends TelegramLongPollingBot {

    private int tasksQuantity = 0; //кол-во заданий
    private List<String> correctAnswers = new ArrayList<>(); //правильные ответы
    private boolean settingDzNow = false; //будет ли будущее соообщение количеством заданий
    private Long tutorChatId = null;
    private Long studentChatId = null;
    private boolean waitingDzText = false;
    private List<Integer> checkAnswers = new ArrayList<>();
    private int ribki = 0;
    private int[] lots = {140, 160, 90, 160, 80, 50, 140, 60, 90, 30};



    @Override
    public String getBotUsername() {
        return "exambotoop_bot";
    }

    @Override
    public String getBotToken() {
        return "8350454031:AAEu8fa4NrLxeDzKXeWEg7-cn27UVk6TOn0";
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(!update.hasMessage()||!update.getMessage().hasText()){
            return;
        }

        String text = update.getMessage().getText().trim();
        System.out.println(text);
        Long chatId = update.getMessage().getChatId();


        if(text.equals("/start")){
            sendMessage(chatId,"привет! Если ты ученик, нажми на /iamstudent");
            return;
        }

        if(text.equals("/iamstudent")){
            studentChatId = chatId;
            sendMessage(chatId,"Отлично!Теперь это твой бот. Уведомление о новой домашке придет тебе! Начинай ее выполнять" +
                    "по порядку, в формате /numНОМЕР ответ, пример: /num1 30\n Чтобы посмотреть, сколько осталось сделать или закончить" +
                    "выполнять задание, напиши /results \n Чтобы узнать подробнее про рыбки, нажми /ribki");
            return;
        }

        if(text.equals("/iamtutor")){
            tutorChatId = chatId;
            sendMessage(tutorChatId, " Добрый день, уважаемый прекрасный самый лучший на свете преподаватель. Вот инструкция к боту, советуем закрепить\n" +
                    "\n" +
                    "ДОМАШНЕЕ ЗАДАНИЕ:\n" +
                    "/setdz - назначить количество номеров в работе\n" +
                    "/setanswer номер ответ - установить ответ для номера в домашней работе\n" +
                    "/newdz - отправить ученику домашнюю работу\n" +
                    "\n" +
                    "РЫБКИ\n" +
                    "/niceWorkAndAnswer - работа на занятии + доп.вопрос\n" +
                    "/niceWork -  работа на занятии без доп.вопроса\n" +
                    "/fastHomework - ускоренное дз\n" +
                    "/late - опоздание на урок\n" +
                    "/noDz - невыполненное дз\n" +
                    "/lateHomework - поздняя домашка\n" +
                    "/dzDone - ручное завершение домашней работы\n" +
                    "/reminder - напомнит, что нужно сделать дз");
            return;
        }

        //setdz устанавливает количество заданий на следующую домашку.при вызове команды он меняет булеву переменную на true
        // и следующее сообщение, которое введет пользователь - будет количеством номеров в домашней работе.
        //бывает такое, что человек случайно ввел команду, он может ввести любой текст и кол-во заданий не установится.
        //мы не будем "допытывать" репетитора, поэтому попытка ввести число одна. Захочет - напишет /setdz еще раз :)
        if(text.equals("/setdz")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            settingDzNow = true;
            sendMessage(chatId, "Сколько заданий?");
            return;
        }

        //относится к setdz, сохраняем число заданий
        if(settingDzNow){
            settingDzNow = false;
            Integer n = parseInt(text);
            if(n==null || n<1||n>10){
                sendMessage(chatId, "Нужно число от 1 до 10, нажмите на /setdz, чтобы ввести еще раз");
                return;
            }
            //увеличиваем или уменьшаем размер массива правильных ответов в зависимости от того, сколько номеров в домашке
            if(n>correctAnswers.size()){
                while (correctAnswers.size()<n) correctAnswers.add(null);
            }else{
                while (correctAnswers.size()>n) correctAnswers.remove(correctAnswers.size()-1);
            }
            //тоже самое для массива, где будет храниться, верно или неверно сделан номер
            if(n>checkAnswers.size()){
                while (checkAnswers.size()<n) checkAnswers.add(null);
            }else{
                while (checkAnswers.size()>n) checkAnswers.remove(checkAnswers.size()-1);
            }

            tasksQuantity = n;
            sendMessage(chatId, "Установлено заданий: " + tasksQuantity);
        }


        //хотим уже отправить новую домашнюю работу (после того как ввели ответы и количество номеров)
        if(text.equals("/newdz")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }

            //обнуляем результаты ученика с предыдущего дз
            checkAnswers.clear();
            for(int i = 0; i<tasksQuantity; i++){
                checkAnswers.add(0);
            }

            waitingDzText = true;
            sendMessage(chatId, "Введите текст домашнего задания");

            return;
        }
        if(waitingDzText){
            waitingDzText = false;
            String dz = text;
            sendMessage(studentChatId,"Появилось новое домашнее задание, начинай выполнять!"
                + text + "\n Дать ответ на номер 1 - /num1 ответ");
            sendMessage(tutorChatId, "Домашняя работа отправлена");
            return;
        }

        //устанавливаем ответы (учитель)
        if(text.startsWith("/setanswer")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            if(tasksQuantity<=0){
                sendMessage(chatId, "Сначала задайте количество заданий через /setdz");
                return;
            }

            //в установке ответа должно быть 3 части - /setanswer номер ответ
            String[] parts = text.trim().split("\\s+"); //используем \\s+, а не " ", потому что человек может случайно поставить несколько пробелов
            if(parts.length!=3){
                sendMessage(chatId,"Введите в формате /setanswer номер ответ");
                return;
            }

            String taskNumber = parts[1];
            if(!taskNumber.matches("\\d+")){
                sendMessage(chatId, "Номер задания должен быть цифрой");
                return;
            }
            int taskNumberInt = Integer.parseInt(taskNumber);
            if(taskNumberInt<1 || taskNumberInt>tasksQuantity){
                sendMessage(chatId, "Нет такого номера, всего в домашней работе номеров: " + tasksQuantity);
                return;
            }

            String answer = parts[2];
            correctAnswers.set(taskNumberInt-1, answer);
            sendMessage(chatId, answer + " ответ на задание " + taskNumber + " зафиксирован");
           return;
        }


        //устанавливаем ответы (ученик)
        if(text.startsWith("/num")){
            String[] parts = text.trim().split("\\s+"); //используем \\s+, а не " ", потому что человек может случайно поставить несколько пробелов
            if(parts.length!=2){
                sendMessage(chatId,"Введите в формате /numНОМЕР ответ");
                return;
            }
            String taskNumber = parts[0].substring(4); //то что после /num
            if(!taskNumber.matches("\\d+")){
                sendMessage(chatId, "Номер задания должен быть цифрой");
                return;
            }
            int taskNumberInt = Integer.parseInt(taskNumber);
            if(taskNumberInt<1 || taskNumberInt>tasksQuantity){
                sendMessage(chatId, "Нет такого номера, всего в домашней работе номеров: " + tasksQuantity);
                return;
            }

            String studentAnswer = parts[1];
            String correct = correctAnswers.get(taskNumberInt-1);

            if(correct!=null && studentAnswer.equals(correct)){
                checkAnswers.set(taskNumberInt - 1, 1);
                sendMessage(chatId, "Верно! Ты молодец");
                sendMessage(tutorChatId, "Выполнен правильно номер "+ taskNumber);
            }else{
                checkAnswers.set(taskNumberInt-1,0);
                sendMessage(chatId, "Неверно, попробуй еще");
                sendMessage(tutorChatId, "Выполнен неправильно номер "+ taskNumber +" ответ ученика:" + studentAnswer);
            }
        }

        //обрабатываем /results, выводит ученику, что либо все сделано, либо какие номера остались/неверные
        if(text.equals("/results")){
            boolean allCorrect = true;
            String notDone ="Недоделанные номера: ";
            for(int i = 0; i<tasksQuantity;i++){
                if(checkAnswers.get(i)!=1){ //задание не сделано
                    allCorrect = false;
                    notDone+=(i+1) + " ";
                }
            }
            if(allCorrect){
                sendMessage(chatId, "Все номера правильные, отдыхай!");
                //обнуляем всё сразу после выполненной домашки
                for(int i = 0; i<checkAnswers.size();i++){
                    checkAnswers.set(i,0);
                }
                ribki+=15;
                sendMessage(tutorChatId, "Ученик полностью сделал домашнюю работу");
                return;
            }else{
                sendMessage(chatId, notDone);
                sendMessage(tutorChatId, "Прогресс ученика сейчас: \n" + notDone);
                return;
            }
        }

        //дальше пойдет всё, связанное с рыбками

        //для репетитора:
        if(text.equals("/niceWorkAndAnswer")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki+=15;
            sendMessage(tutorChatId,"рыбки успешно зачислены");
            sendMessage(studentChatId,"Привет! Сегодня ты отлично поработала на занятии и ответил на доп.вопрос вначале!Рыбки уже зачислены тебе на баланс");
            return;
        }
        if(text.equals("/niceWork")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki+=10;
            sendMessage(tutorChatId,"рыбки успешно зачислены");
            sendMessage(studentChatId,"Сегодня ты хорошо поработала на занятии!Рыбки уже зачислены тебе на баланс!");
            return;
        }

        if(text.equals("/fastHomework")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki+=15;
            sendMessage(tutorChatId,"рыбки успешно зачислены");
            sendMessage(studentChatId,"За ускоренное выполнение домашней работы  рыбки уже зачислены тебе на баланс!");
            return;
        }
        if(text.equals("/late")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki-=13;
            sendMessage(tutorChatId,"рыбки забрали");
            sendMessage(studentChatId,"Сегодня ты опоздала на занятие. К сожалению, часть рыбок придется забрать(");
            return;
        }
        if(text.equals("/noDz")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki-=30;
            sendMessage(tutorChatId,"рыбки забрали");
            sendMessage(studentChatId,"Сегодня ты не выполнила домашнюю работу( Часть рыбок сняты с твоего баланса");
            return;
        }
        if(text.equals("/lateHomework")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki-=15;
            sendMessage(tutorChatId,"рыбки забрали");
            sendMessage(studentChatId,"К сожалению, домашка поступила от тебя очень поздно( Придется забрать часть рыбок");
            return;
        }
        if(text.equals("/dzDone")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            ribki+=15;
            for(int i = 0; i<checkAnswers.size();i++){
                checkAnswers.set(i,0);
            }
            sendMessage(tutorChatId,"рыбки успешном начислили");
            sendMessage(studentChatId,"Твоя домашка засчитана, можешь отдыхать");
            return;
        }
        if(text.equals("/reminder")){
            if(!isTutor(chatId)){
                sendMessage(chatId,"Команда вам недоступна");
                return;
            }
            sendMessage(tutorChatId,"напомнили о домашней работе");
            sendMessage(studentChatId,"До занятия осталось мало времени, а домашка еще не сделана(Пора поторопиться");
            return;
        }


        //для ученика
        if(text.equals("/ribki")){
            sendMessage(chatId,"\"Рыбки\" \uD83D\uDC21 - валюта, которая используется на наших занятиях\n" +
                    "\n" +
                    "Их ты можешь обменивать на различные бонусы.\n" +
                    "На какие именно? /market_ribki\n" +
                    "\n" +
                    "Как можно их \"зарабатывать\"? За своевременное выполнение домашки и активную работу на уроках. Но также действует система \"штрафов\", из-за которой рыбок могут забрать. Подробнее /rules_ribki\n" +
                    "\n" +
                    "Чтобы узнать свой баланс /bal_ribki");
            return;
        }

        if(text.equals("/bal_ribki")){
            sendMessage(chatId,"Твой баланс рыбок: " + ribki);
            return;
        }

        if(text.equals("/rules_ribki")){
            sendMessage(chatId,"За что же начисляются рыбки? \n" +
                    "\n" +
                    "1: Выполнение домашнего задания:\n" +
                    "в первые 24 часа после урока: 30\uD83D\uDC21 \n" +
                    "(если урок начинается в 17:00 в четверг, то задание должно быть засчитано ботом до 17:00 пятницы, при возникновении трудностей - пишите мне, но понимайте, что моментально я могу не ответить, поэтому начинать в 16:40 в пятницу - плохая идея :) )\n" +
                    "(если занятия идут 2 дня подряд, то в первые 16 часов)\n" +
                    "\n" +
                    "менее чем за 30 минут до занятия: 5\uD83D\uDC21\n" +
                    "\n" +
                    "в любое другое время: 15\uD83D\uDC21\n" +
                    "ОЧЕНЬ ВАЖНО: некоторые задания вы можете не сделать, это нормально. Если после моего объяснения все равно не получается сделать какой-то номер/номера, то рыбки вам всё равно зачислятся. При выполнении домашнего задания вам автоматически начислится 19 рыбок, но если вы сдали раньше/позже, то их количество увеличится/уменьшится в течение некоторого времени\n" +
                    "\n" +
                    "2: За ответ на мой рандомный вопрос в начале занятия по предыдущим темам урока (я могу спросить и то, что мы проходили 2 месяца назад): 5\uD83D\uDC21\n" +
                    "\n" +
                    "3: За активную работу на занятии: до 10 \uD83D\uDC21\n" +
                    "\n" +
                    "4: За решение дополнительного домашнего задания (оно чаще всего небольшое, на повторение или более качественное закрепление темы, его решение - по желанию) : 16\uD83D\uDC21 \n" +
                    "\n" +
                    "Штрафы:\n" +
                    "\n" +
                    "Опоздание на урок более чем на 5 минут: -13\uD83D\uDC21 \n" +
                    "\n" +
                    "Невыполнение домашней работы: -30\uD83D\uDC21 \n" +
                    "\n" +
                    "Попытка обмануть бота и накрутить рыбок - обнуление баланса \uD83D\uDC21 \n" +
                    "\n" +
                    "Посторонние дела во время урока (телефон, игры, чат с друзьями: -12\uD83D\uDC21\n" +
                    "ОЧЕНЬ ВАЖНО:\n" +
                    "Опять же, бывают очень разные обстоятельства, и если вдруг вы опоздали или не сделали домашку по уважительной причине (ОБЯЗАТЕЛЬНО писать заранее) или срочно нужно 1 раз ответить на звонок/сообщение, то ничего страшного, за это штрафов не будет ");
            return;
        }

        if(text.equals("/market_ribki")){
            sendMessage(chatId,"Все эти бонусы вы можете покупать как до занятия и использовать в любое время, так и на них (* — нельзя купить на занятии)\n" +
                    "\n" +
                    "Для покупки напиши команду для нужного лота!\n" +
                    "\n" +
                    "\"Отдых\": нет дз на следующий урок (действует только на следующее домашнее задание): 140 \uD83D\uDC21 /lot1\n" +
                    "\n" +
                    "\"Тайная коробочка*\": секретный подарок: 160 \uD83D\uDC21/lot2\n" +
                    "\n" +
                    "\"Колесо фортуны\": может выпасть что угодно из этого списка + 2 наказания: 90 \uD83D\uDC21 /lot3\n" +
                    "\n" +
                    "\"Игровой день*\": урок в форме игры: 160 \uD83D\uDC21 /lot4\n" +
                    "\n" +
                    "\"Кубик везения\": количество дз определяет кубик (может выпасть число от 1 до 6. Для сложных и средних номеров это количество остается (выпало 3 — значит 3 номера на домашку), для легких — умножается на 2 (выпало 3 — значит 6 номеров на дз)): 80 \uD83D\uDC21 /lot5\n" +
                    "\n" +
                    "\"Полезный перерыв\": 5-минутный полезный перерыв во время урока (не просто листать тик ток): 50 \uD83D\uDC21 /lot6\n" +
                    "\n" +
                    "\"Ленивый день\": урок, на котором будет либо повторение, либо легкая тема: 140 \uD83D\uDC21 /lot7\n" +
                    "\n" +
                    "\"Не хочу логику\": творческая домашка (нельзя использовать больше 1 раза в месяц): 60 \uD83D\uDC21 /lot8\n" +
                    "\n" +
                    "\"Двойная добыча\": двойные рыбки на 3 занятия : 90 \uD83D\uDC21 /lot9\n" +
                    "\n" +
                    "\"Пропуск\": можно пропустить 1 номер на уроке, который не понравится: 30 \uD83D\uDC21 /lot10");
            return;
        }

        if(text.startsWith("/lot")){
            String lotNumStr = text.substring(4);
            if((!lotNumStr.matches("\\d+"))){ //проверяем вся ли строка - число
                sendMessage(chatId, "Неверный номер лота");
                return;
            }
            int lotNum = Integer.parseInt(lotNumStr);
            if(lotNum<1||lotNum>lots.length){
                sendMessage(chatId, "Нет такого лота");
                return;
            }

            int price = lots[lotNum-1];
            if(ribki>=price){
                ribki-=price;
                sendMessage(chatId, "Покупка лота " + lotNum + " проведена успешно!");
                sendMessage(tutorChatId, "Ученик купил лот " + lotNum);
                return;
            }else{
                sendMessage(chatId, "Недостаточно средств. Нужно еще рыбок: " + (price-ribki));
            }
            return;
        }





    }

    private boolean isTutor(Long chatId) {
        return tutorChatId!=null && tutorChatId.equals(chatId);
    }




    //отправка сообщений
    private void sendMessage(Long chatId, String text) {
        try{
            SendMessage msg = SendMessage.builder().chatId(chatId.toString())
                    .text(text).build();
            execute(msg);
        } catch (TelegramApiException e){
            throw new RuntimeException(e);
        }
    }
}
