// Сборка презентации по лабораторной работе № 2 (pptxgenjs)
const path = require('path');
const fs = require('fs');
const pptxgen = require('pptxgenjs');

const PROJECT = '/Users/azatminyazov/Workspace/university java/2 work';
const OUT = process.env.OUT || path.join(PROJECT, 'docs/presentation/Презентация_ЛР2_Минязов.pptx');
const shot = n => path.join(PROJECT, 'docs/screenshots', n);
const uml = n => path.join(PROJECT, 'docs/uml', n);

// Палитра окна программы: тёмный графит, зелёный путь, вода, ток, сыр
const C = {
  dark: '2C2C2A', ink: '2C2C2A', muted: '6B6A65', light: 'F7F6F3', line: 'E4E2DC',
  path: '1D9E75', pathBg: 'E1F5EE', water: '185FA5', waterBg: 'E6F1FB',
  shock: 'A32D2D', shockBg: 'FCEBEB', cheese: 'BA7517', cheeseBg: 'FAEEDA', white: 'FFFFFF',
};
const HEAD = 'Cambria';
const BODY = 'Calibri';

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

/** Картинка, вписанная в прямоугольник с сохранением пропорций (по центру). */
function fit(slide, file, x, y, w, h, extra = {}) {
  const s = pngSize(file);
  const k = Math.min(w / s.w, h / s.h);
  const iw = s.w * k, ih = s.h * k;
  slide.addImage({ path: file, x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih, ...extra });
  return { x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih };
}

function title(slide, text, sub) {
  slide.addText(text, { x: 0.6, y: 0.4, w: 12.1, h: 0.8, fontFace: HEAD, fontSize: 36, bold: true, color: C.ink, margin: 0, isTextBox: true });
  if (sub) {
    slide.addText(sub, { x: 0.6, y: 1.15, w: 12.1, h: 0.45, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  }
}

function card(slide, x, y, w, h, fill) {
  slide.addShape('roundRect', { x, y, w, h, fill: { color: fill }, line: { color: fill }, rectRadius: 0.12 });
}

/** Кружок с цифрой или знаком — общий мотив всех слайдов. */
function badge(slide, x, y, d, fill, text, color = C.white, size = 16) {
  slide.addShape('ellipse', { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  slide.addText(text, { x, y, w: d, h: d, align: 'center', valign: 'middle', fontFace: BODY, fontSize: size, bold: true, color, margin: 0, isTextBox: true });
}

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE';  // 13.33 x 7.5
pres.author = 'А. А. Минязов';
pres.title = 'Мышь в лабиринте — обучение с подкреплением';

// ---------- 1. Титул ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Лабораторная работа № 2 · ООП на языке Java', { x: 0.6, y: 0.7, w: 6.4, h: 0.4, fontFace: BODY, fontSize: 16, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addText('Мышь в лабиринте', { x: 0.6, y: 1.5, w: 6.4, h: 1.0, fontFace: HEAD, fontSize: 44, bold: true, color: C.white, margin: 0, isTextBox: true });
  s.addText('Обучение с подкреплением: мышь без карты учится находить сыр, пить воду и обходить ток', { x: 0.6, y: 2.6, w: 6.2, h: 1.1, fontFace: BODY, fontSize: 20, color: 'D3D1C7', margin: 0, isTextBox: true });
  s.addText([
    { text: 'Выполнил: ', options: { color: 'B4B2A9' } }, { text: 'А. А. Минязов, гр. 5130902/40202', options: { color: C.white, breakLine: true } },
    { text: 'Проверил: ', options: { color: 'B4B2A9' } }, { text: 'ст. преп. Д. С. Хасанов', options: { color: C.white, breakLine: true } },
    { text: 'СПбПУ, ИКНК · 2026', options: { color: 'B4B2A9' } },
  ], { x: 0.6, y: 5.4, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 16, margin: 0, paraSpaceAfter: 4, isTextBox: true });
  fit(s, shot('gui_2_trained.png'), 7.2, 0.9, 5.6, 5.7, { rounding: false });
  s.addNotes('Тема: обучение с подкреплением на примере мыши в лабиринте. Мышь не видит карту, учится только по наградам от среды. Программа на Java, окно на JavaFX.');
}

// ---------- 2. Задача ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Задача', 'Задача 1.11 «Мышь в лабиринте»');
  s.addText([
    { text: 'На каждом шаге мышь выбирает направление, а среда сообщает выигрыш или проигрыш.', options: { bullet: true, breakLine: true } },
    { text: 'Без этой обратной связи мышь не знает, какое действие лучше.', options: { bullet: true, breakLine: true } },
    { text: 'Мышь знает сумму выигрыша за попытку.', options: { bullet: true, breakLine: true } },
    { text: 'Лабиринт любого размера; размер и схему задаёт пользователь.', options: { bullet: true } },
  ], { x: 0.6, y: 1.9, w: 5.6, h: 3.4, fontFace: BODY, fontSize: 18, color: C.ink, paraSpaceAfter: 12, margin: 0, valign: 'top', isTextBox: true });

  const items = [
    ['+100', 'Сыр', 'самая большая награда (+Z), попытка заканчивается', C.cheese, C.cheeseBg],
    ['+10', 'Вода', 'награда поменьше (+x), выпивается до конца попытки', C.water, C.waterBg],
    ['−50', 'Ток', 'электротравма (−y), бьёт каждый раз', C.shock, C.shockBg],
    ['−1', 'Шаг', 'плата за каждый шаг: короткий путь выгоднее', C.muted, C.light],
  ];
  items.forEach(([v, name, desc, col, bg], i) => {
    const y = 1.9 + i * 1.2;
    card(s, 6.8, y, 5.9, 1.0, bg);
    badge(s, 7.0, y + 0.15, 0.7, col, v, C.white, 15);
    s.addText(name, { x: 7.9, y: y + 0.12, w: 4.6, h: 0.4, fontFace: BODY, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(desc, { x: 7.9, y: y + 0.5, w: 4.6, h: 0.4, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  });
  s.addText('Удар о стену: −5, мышь остаётся на месте', { x: 6.8, y: 6.75, w: 5.9, h: 0.35, fontFace: BODY, fontSize: 12, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Награды из условия: сыр +Z, вода +x, ток −y. Добавлены плата за шаг (чтобы мышь торопилась) и штраф за удар о стену. Вода выпивается, иначе мышь бегала бы туда-обратно у воды бесконечно.');
}

// ---------- 3. Как учится мышь ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Как учится мышь: Q-learning', 'Один шаг — четыре действия; попыток — сотни');
  const steps = [
    ['1', 'Выбор', 'обычно — стрелка с наибольшим числом, иногда (ε) — наугад'],
    ['2', 'Шаг', 'мышь идёт в соседнюю клетку или бьётся о стену'],
    ['3', 'Награда', 'среда сообщает число: сыр, вода, ток, плата за шаг'],
    ['4', 'Обучение', 'мышь исправляет одно число — у стрелки, по которой прошла'],
  ];
  steps.forEach(([n, name, desc], i) => {
    const x = 0.6 + i * 3.1;
    card(s, x, 1.95, 2.8, 2.3, C.light);
    badge(s, x + 0.2, 2.15, 0.6, C.path, n, C.white, 18);
    s.addText(name, { x: x + 0.95, y: 2.2, w: 1.8, h: 0.5, fontFace: BODY, fontSize: 20, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(desc, { x: x + 0.2, y: 2.95, w: 2.45, h: 1.2, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
    if (i < 3) {
      s.addText('→', { x: x + 2.8, y: 2.8, w: 0.3, h: 0.5, fontFace: BODY, fontSize: 22, color: C.path, align: 'center', margin: 0, isTextBox: true });
    }
  });
  card(s, 0.6, 4.6, 12.1, 1.3, C.pathBg);
  s.addText('Q[s][a] ← Q[s][a] + α · (r + γ · max Q[s′] − Q[s][a])', { x: 0.9, y: 4.75, w: 11.5, h: 0.6, fontFace: HEAD, fontSize: 26, italic: true, color: '085041', margin: 0, isTextBox: true });
  s.addText('«Шаг хорош, если за него сразу дали награду или он привёл туда, откуда уже известен хороший путь»', { x: 0.9, y: 5.35, w: 11.5, h: 0.45, fontFace: BODY, fontSize: 15, color: '0F6E56', margin: 0, isTextBox: true });
  s.addText([
    { text: 'α = 0.2', options: { bold: true } }, { text: ' — сила одного исправления   ' },
    { text: 'γ = 0.995', options: { bold: true } }, { text: ' — доля ценности, сохраняемая за шаг   ' },
    { text: 'ε: 30% → 1%', options: { bold: true } }, { text: ' — доля случайных шагов' },
  ], { x: 0.6, y: 6.2, w: 12.1, h: 0.5, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, isTextBox: true });
  s.addNotes('Q-таблица — числа на стрелках: для каждой клетки и направления — сколько очков в сумме мышь получит, если пойдёт туда. После каждого шага правится одно число. Ценность сыра с каждой попыткой уходит на шаг дальше от сыра, пока не дойдёт до старта. Случайные шаги нужны, чтобы находить обходные пути.');
}

// ---------- 4. Числа на стрелках ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Что мышь выучила', 'Стрелка в каждой клетке — направление с наибольшим числом Q');
  const box = fit(s, shot('gui_3_arrows.png'), 0.6, 1.8, 7.6, 5.2);
  const x = 8.6;
  const stats = [
    ['112', 'попыток до выученного маршрута в лабиринте 12×9', C.path],
    ['−2500 → +81', 'сумма за попытку: в начале и после обучения', C.ink],
  ];
  stats.forEach(([v, l, col], i) => {
    s.addText(v, { x, y: 1.9 + i * 1.7, w: 4.2, h: 0.8, fontFace: HEAD, fontSize: 40, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(l, { x, y: 2.7 + i * 1.7, w: 4.2, h: 0.6, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  });
  card(s, x, 5.4, 4.2, 1.5, C.pathBg);
  s.addText('Зелёная линия — путь без случайных шагов. Кратчайший путь идёт через ток внизу; мышь выучила обход и заходит за водой.', { x: x + 0.2, y: 5.5, w: 3.8, h: 1.3, fontFace: BODY, fontSize: 14, color: '085041', margin: 0, isTextBox: true });
  s.addNotes('На снимке включены стрелки. Мышь не видела карту, но выучила обход тока: прямой путь дороже на 50 очков, обход — всего несколько лишних шагов по −1.');
}

// ---------- 5. Лабиринт и редактор ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Лабиринт: генерация и редактор', 'Размер до 100×100, схему задаёт пользователь');
  const steps = [
    ['1', 'Идеальный лабиринт', 'обход в глубину: к каждой клетке ровно один путь'],
    ['2', 'Обходные пути', 'убирается заданный процент стен — мыши есть из чего выбирать'],
    ['3', 'Вода и ток', 'в случайные клетки, кроме старта и сыра; один seed — один лабиринт'],
    ['4', 'Редактор', 'клик по границе — стена; клик в клетку — вода, ток, старт, сыр'],
  ];
  steps.forEach(([n, name, desc], i) => {
    const y = 1.85 + i * 1.25;
    badge(s, 0.6, y, 0.55, C.path, n, C.white, 16);
    s.addText(name, { x: 1.35, y: y - 0.02, w: 4.4, h: 0.4, fontFace: BODY, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(desc, { x: 1.35, y: y + 0.38, w: 4.4, h: 0.7, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  });
  fit(s, shot('gui_4_editor.png'), 6.0, 1.75, 6.8, 5.3);
  s.addNotes('Генератор: сначала все стены, обход в глубину пробивает проходы — лабиринт с единственным путём. Потом сносим часть стен, чтобы появились обходы: без выбора нечему учиться. Редактор меняет схему; после правки обучение начинается заново.');
}

// ---------- 6. Подбор параметров ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Подбор параметров', '936 наборов на 30 лабиринтах; проверка — на 60 других до 30×30');
  s.addChart(pres.charts.BAR, [{ name: 'Лабиринтов из 30', labels: ['0.9', '0.95', '0.98', '0.99'], values: [13.9, 15.0, 21.8, 24.7] }], {
    x: 0.6, y: 1.8, w: 6.0, h: 4.6, barDir: 'col',
    showTitle: true, title: 'Мышь нашла путь при разных γ (из 30)', titleFontFace: BODY, titleFontSize: 15, titleColor: C.ink,
    showValue: true, dataLabelPosition: 'outEnd', dataLabelFontSize: 13, dataLabelColor: C.ink, dataLabelFormatCode: '0.0',
    chartColors: [C.path], showLegend: false,
    catAxisLabelColor: C.muted, valAxisLabelColor: C.muted, catAxisLabelFontSize: 13, valAxisLabelFontSize: 11,
    valAxisMinVal: 0, valAxisMaxVal: 30, valGridLine: { color: C.line, size: 0.5 }, catGridLine: { style: 'none' },
    catAxisTitle: 'γ', showCatAxisTitle: true, catAxisTitleColor: C.muted, catAxisTitleFontSize: 13,
  });
  const x = 7.2;
  card(s, x, 1.8, 5.5, 2.2, C.light);
  s.addText('Проверочные лабиринты', { x: x + 0.3, y: 1.95, w: 5, h: 0.4, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  s.addText([
    { text: '25', options: { color: C.shock } }, { text: ' → ', options: { color: C.muted } }, { text: '60', options: { color: C.path } },
    { text: ' из 60', options: { color: C.muted, fontSize: 22 } },
  ], { x: x + 0.3, y: 2.4, w: 5, h: 1.0, fontFace: HEAD, fontSize: 48, bold: true, margin: 0, isTextBox: true });
  s.addText('мышь нашла путь: пробные значения → подобранные', { x: x + 0.3, y: 3.4, w: 5, h: 0.4, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  s.addText([
    { text: 'Главный фактор — γ: при γ = 0.9 сыр за 70 шагов стоит для мыши 0.06 очка — меньше одного шага.', options: { bullet: true, breakLine: true } },
    { text: 'При γ = 0.995 тот же сыр стоит около 70 очков, и его ценность доходит до старта.', options: { bullet: true, breakLine: true } },
    { text: 'Итог: α = 0.2, γ = 0.995, лимит 4000 шагов.', options: { bullet: true } },
  ], { x, y: 4.3, w: 5.5, h: 2.6, fontFace: BODY, fontSize: 15, color: C.ink, paraSpaceAfter: 10, margin: 0, valign: 'top', isTextBox: true });
  s.addNotes('Награды заданы условием, подбирались только параметры обучения. Перебор: 720 + 216 наборов. Лучший набор проверен на 60 лабиринтах, не участвовавших в подборе: путь найден во всех 60, со старыми пробными значениями — в 25.');
}

// ---------- 7. Архитектура ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Архитектура', 'Модель — контроллер — окно; модель и логика кнопок не зависят от JavaFX');
  const cols = [
    ['maze.model', 'Maze, MazeGenerator — лабиринт\nAttempt — среда, награды\nQTable, Mouse — Q-learning\nTrainer — попытки, автостоп', C.path, C.pathBg],
    ['maze.controller', 'TrainingController — кнопки, скорость\nMazeEditor — редактор\nSettingsForm, MazeForm — проверка ввода', C.water, C.waterBg],
    ['maze.view', 'MazeApp — главное окно\nMazeCanvas — рисование\nScoreChart — график\nFormDialog — окна параметров', C.cheese, C.cheeseBg],
  ];
  cols.forEach(([name, text, col, bg], i) => {
    const x = 0.6 + i * 4.1;
    card(s, x, 1.9, 3.8, 2.6, bg);
    s.addText(name, { x: x + 0.3, y: 2.1, w: 3.3, h: 0.5, fontFace: HEAD, fontSize: 22, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(text, { x: x + 0.3, y: 2.75, w: 3.3, h: 2.3, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, paraSpaceAfter: 6, valign: 'top', isTextBox: true });
  });
  s.addText([
    { text: 'Инкапсуляция: ', options: { bold: true } }, { text: 'стены и предметы меняются только методами Maze; Settings неизменяем и проверяет себя.', options: { breakLine: true } },
    { text: 'Наследование и полиморфизм: ', options: { bold: true } }, { text: 'MazeApp → Application, MazeCanvas → Pane; sealed-типы результатов, обобщённый FormDialog<T>.' },
  ], { x: 0.6, y: 4.9, w: 12.1, h: 1.3, fontFace: BODY, fontSize: 15, color: C.ink, paraSpaceAfter: 8, margin: 0, valign: 'top', isTextBox: true });
  s.addNotes('Модель ничего не знает об окне. Логика кнопок вынесена в контроллер, поэтому её можно проверить без открытия окна. Окно только передаёт нажатия и рисует.');
}

// ---------- 8. UML ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'UML-диаграммы', 'Шесть видов диаграмм, построены в PlantUML');
  const list = [
    ['usecase.png', 'Прецеденты'], ['activity_attempt.png', 'Деятельность'], ['class_model.png', 'Классы'],
    ['sequence_step.png', 'Взаимодействие'], ['state_training.png', 'Состояния'], ['component.png', 'Компоненты'],
  ];
  list.forEach(([file, name], i) => {
    const col = i % 3, row = Math.floor(i / 3);
    const x = 0.6 + col * 4.1, y = 1.8 + row * 2.7;
    card(s, x, y, 3.8, 2.45, C.light);
    fit(s, uml(file), x + 0.15, y + 0.12, 3.5, 1.8);
    s.addText(name, { x: x + 0.15, y: y + 1.98, w: 3.5, h: 0.38, fontFace: BODY, fontSize: 15, bold: true, color: C.ink, align: 'center', margin: 0, isTextBox: true });
  });
  s.addNotes('Диаграммы в отчёте, раздел 3. Прецеденты — что делает пользователь и мышь. Деятельность — одна попытка мыши и клик в редакторе. Классы — модель и окно. Взаимодействие — кадр окна при обучении. Состояния — мышь в попытке и режимы обучения. Компоненты — пакеты модуля.');
}

// ---------- 9. Демонстрация ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Демонстрация', 'Первая попытка и после «Обучить 500»');
  fit(s, shot('gui_1b_first_attempt.png'), 0.6, 1.8, 5.9, 4.3);
  fit(s, shot('gui_2_trained.png'), 6.8, 1.8, 5.9, 4.3);
  s.addText('Мышь ничего не знает: бродит, бьётся о стены; за 59 шагов −231', { x: 0.6, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  s.addText('Выучила за 112 попыток: обходит ток, пьёт воду; график растёт', { x: 6.8, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: C.path, bold: true, margin: 0, isTextBox: true });
  s.addNotes('Живая демонстрация: Старт на малой скорости — видно случайные шаги. Затем «Обучить 500» — появляется зелёный путь и надпись «выучила маршрут». Можно включить стрелки, поставить молнию в редакторе на путь и обучить заново — мышь найдёт новый обход.');
}

// ---------- 10. Защита от ввода ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Защита от некорректного ввода', 'Программа не падает и объясняет ошибку');
  s.addText([
    { text: 'Пусто, буквы, 1e3, NaN, Infinity, цифры других алфавитов, огромные числа, управляющие символы', options: { bullet: true, breakLine: true } },
    { text: 'Смысл: сыр — самая большая награда, ток не награда, α в (0; 1], воды и тока не больше свободных клеток', options: { bullet: true, breakLine: true } },
    { text: 'Все ошибки сразу, поля подсвечены, окно не закрывается', options: { bullet: true, breakLine: true } },
    { text: 'Редактор: границу не стереть, на старт и сыр ничего не положить, «сыр недостижим» — предупреждение', options: { bullet: true, breakLine: true } },
    { text: '«Обучить 100 000» на 100×100 — доли секунды: окно не зависает', options: { bullet: true } },
  ], { x: 0.6, y: 1.9, w: 6.6, h: 5.0, fontFace: BODY, fontSize: 16, color: C.ink, paraSpaceAfter: 12, margin: 0, valign: 'top', isTextBox: true });
  fit(s, shot('gui_5_settings_errors.png'), 7.6, 1.8, 5.1, 5.3);
  s.addNotes('Настройки, при которых мышь почти наверняка не выучит маршрут (бесплатные шаги при γ около 1), не запрещены, но окно предупреждает и применяет их только по второму нажатию. Причина — мышь не знает, какая дальняя вода уже выпита.');
}

// ---------- 11. Итоги ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Итоги', { x: 0.6, y: 0.6, w: 12, h: 0.9, fontFace: HEAD, fontSize: 40, bold: true, color: C.white, margin: 0, isTextBox: true });
  const res = [
    ['Q-learning', 'мышь без карты учится по наградам и знает сумму выигрыша за попытку'],
    ['Лабиринт', 'любого размера до 100×100, случайная генерация и редактор схемы'],
    ['Параметры', 'подобраны перебором: путь найден в 60 из 60 проверочных лабиринтов'],
    ['ООП и UML', 'модель — контроллер — окно; шесть видов UML-диаграмм'],
  ];
  res.forEach(([name, text], i) => {
    const y = 1.9 + i * 1.2;
    badge(s, 0.6, y, 0.6, C.path, String(i + 1), C.white, 18);
    s.addText(name, { x: 1.45, y: y - 0.02, w: 2.2, h: 0.6, fontFace: BODY, fontSize: 22, bold: true, color: C.white, margin: 0, valign: 'middle', isTextBox: true });
    s.addText(text, { x: 3.7, y: y - 0.02, w: 9.0, h: 0.6, fontFace: BODY, fontSize: 18, color: 'D3D1C7', margin: 0, valign: 'middle', isTextBox: true });
  });
  s.addText('Спасибо за внимание', { x: 0.6, y: 6.6, w: 12, h: 0.5, fontFace: BODY, fontSize: 18, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addNotes('Главное: мышь учится на обратной связи, как требует условие; программа устойчива к неверному вводу; параметры подобраны замером, а не наугад.');
}

pres.writeFile({ fileName: OUT }).then(f => console.log('готово:', f));
