'use strict';
const $ = id => document.getElementById(id);
const catalogs={services:'services',projects:'cases',specialists:'specialists',team:'team'};
const categoryKinds={serviceCategories:'services',caseCategories:'cases',specialistCategories:'specialists',teamCategories:'team'};
const sections = {pages:'Страницы',services:'Услуги',serviceCategories:'Категории услуг',projects:'Кейсы',caseCategories:'Категории кейсов',specialists:'Специалисты',specialistCategories:'Категории специалистов',team:'Команда',teamCategories:'Категории команды',articles:'Статьи',categories:'Категории статей',media:'Картинки',leads:'Заявки'};
let auth='', section='pages', rows=[], media=[], categories=[], page=0, totalPages=0, current=null, busy=false, generation=0;
const path = kind => catalogs[kind]?'/api/admin/catalog/'+catalogs[kind]:categoryKinds[kind]?'/api/admin/catalog/'+categoryKinds[kind]+'/categories':'/api/admin/' + (['articles','categories'].includes(kind)?'blog/':'') + kind;
function el(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
function button(text,action){const b=el('button',text);b.type='button';b.onclick=action;return b;}
function notify(message,success=false){$('notice').textContent=message;$('notice').className=success?'success':'';}
async function api(url,method='GET',data){const headers={Authorization:auth};if(data && !(data instanceof FormData))headers['Content-Type']='application/json';const r=await fetch(url,{method,headers,credentials:'omit',body:data?(data instanceof FormData?data:JSON.stringify(data)):undefined});if(!r.ok){let p={};try{p=await r.json();}catch{}if(r.status===401)throw Error('Неверный логин или пароль. Войдите заново.');throw Error(p.detail||p.message||`Ошибка ${r.status}. Попробуйте обновить данные.`);}return r.status===204?null:r.json();}
$('login').onsubmit=async e=>{e.preventDefault();const f=e.target;auth='Basic '+btoa(String.fromCharCode(...new TextEncoder().encode(f.username.value+':'+f.password.value)));const b=f.querySelector('button');b.disabled=true;try{await api(path('categories'));f.reset();f.hidden=true;$('app').hidden=false;$('logout').hidden=false;await load();}catch(e){auth='';notify(e.message);}finally{b.disabled=false;}};
$('logout').onclick=()=>{generation++;auth='';rows=[];media=[];categories=[];current=null;$('list').replaceChildren();$('fields').replaceChildren();$('editor').close();$('app').hidden=true;$('logout').hidden=true;$('login').hidden=false;notify('');};
for(const [key,title] of Object.entries(sections))$('nav').append(button(title,async()=>{section=key;page=0;await load();}));
$('refresh').onclick=()=>load();$('create').onclick=()=>openEditor();$('close').onclick=()=>{if(!busy)$('editor').close();};
$('editor').addEventListener('cancel',e=>{if(busy)e.preventDefault();});
async function load(){const ticket=++generation,kind=section;notify('');$('list').textContent='Загрузка…';$('title').textContent=sections[kind];$('create').hidden=['leads','pages'].includes(kind);[...$('nav').children].forEach((b,i)=>b.classList.toggle('active',Object.keys(sections)[i]===kind));try{const paginated=kind==='articles'||!!catalogs[kind];const data=await api(path(kind)+(paginated?`?page=${page}&size=20`:''));if(ticket!==generation)return;rows=paginated?data.items:data;totalPages=data.totalPages||0;filters();render();}catch(e){if(ticket===generation){$('list').textContent='Не удалось загрузить данные.';notify(e.message);}}}
function filters(){const box=$('filters');box.replaceChildren();const search=el('input');search.placeholder='Поиск';search.id='search';search.oninput=render;box.append(search);}
function render(){const root=$('list');root.replaceChildren();const q=($('search')?.value||'').toLowerCase();const visible=rows.filter(r=>JSON.stringify(r).toLowerCase().includes(q));if(!visible.length)root.append(el('p','Пока ничего нет.','empty'));for(const r of visible){const card=el('div',undefined,'card');if(section==='media'){const img=el('img');img.src=r.url;img.alt=r.altText||'';card.append(img);}const body=el('div',undefined,'body');let title,description;if(section==='pages'){title=r.title;description=`${r.path} · ${r.published?'Опубликовано':'Скрыто'}`;}else if(catalogs[section]){title=r.title;description=`${r.published?'Опубликовано':'Черновик'} · ${r.link}\n${r.description||''}`;}else if(section==='media'){title=r.originalName;description=`${r.altText||'Без описания'} · ${Math.round(r.sizeBytes/1024)} КБ`;}else if(section==='leads'){title=r.name;description=`${r.processed?'Обработана':'Новая'} · ${new Date(r.createdAt).toLocaleString('ru-RU')}\n${r.phone||''} ${r.email||''}\n${r.message||''}`;}else if(section==='articles'){title=r.title;description=`${r.status==='DRAFT'?'Черновик':'Публикация'} · ${r.slug}`;}else{title=r.name;description=r.slug;}body.append(el('strong',title),el('p',description));card.append(body,button(section==='leads'?'Открыть':'Редактировать',()=>section==='pages'?openPage(r.slug):openEditor(r.id)));root.append(card);}if(section==='articles'||catalogs[section]){const p=el('div',undefined,'pager'),prev=button('← Назад',()=>{page--;load();}),next=button('Далее →',()=>{page++;load();});prev.disabled=page===0;next.disabled=page+1>=totalPages;p.append(prev,el('span',`Страница ${page+1} из ${Math.max(1,totalPages)}`),next);root.append(p);}}
function field(name,title,value='',type='text',options){const label=el('label',title);let input;if(options){input=el('select');for(const [v,t] of options)input.append(new Option(t,v));input.value=value??'';}else{input=el(type==='textarea'?'textarea':'input');if(type!=='textarea')input.type=type;if(type==='checkbox')input.checked=!!value;else input.value=value??'';}input.name=name;label.append(input);$('fields').append(label);return input;}
function imageField(name,title,value){
    const options=[['','Не выбрано'],...media.map(m=>[String(m.id),m.originalName+' — '+m.altText])];
    if(value&&!media.some(m=>m.id===value))options.push([String(value),'Выбранная картинка']);
    const select=field(name,title,value||'','select',options);
    attachImageUpload(select);
    return select;
}
function attachImageUpload(select,onUploaded=()=>{}){
    select.classList.add('image-select');
    const tools=el('div',undefined,'image-tools'),file=el('input'),preview=el('img'),status=el('p');
    file.type='file';file.accept='image/jpeg,image/png,image/webp,image/gif,image/avif';file.hidden=true;
    preview.className='image-preview';preview.hidden=true;status.className='image-status';status.setAttribute('role','status');
    function showPreview(){const selected=media.find(m=>String(m.id)===select.value);preview.hidden=!selected;if(selected){preview.src=selected.url;preview.alt=selected.altText||selected.originalName;}else preview.removeAttribute('src');}
    select.addEventListener('change',showPreview);
    const upload=button('Загрузить картинку',()=>{if(!busy)file.click();});upload.className='image-upload';
    tools.append(upload,file,el('small','JPEG, PNG, WebP, GIF, AVIF — до 15 МБ'),preview,status);
    select.parentElement.after(tools);showPreview();
    file.onchange=async()=>{
        const chosen=file.files[0];file.value='';if(!chosen||busy)return;
        status.textContent='';status.classList.remove('error');
        if(chosen.size>15*1024*1024){status.textContent='Файл больше 15 МБ. Выберите меньшую картинку.';status.classList.add('error');return;}
        if(!['image/jpeg','image/png','image/webp','image/gif','image/avif'].includes(chosen.type)){status.textContent='Выберите JPEG, PNG, WebP, GIF или AVIF.';status.classList.add('error');return;}
        const editing=current;lock(true);status.textContent='Загрузка…';
        try{
            const body=new FormData();body.append('file',chosen);body.append('altText',chosen.name.slice(0,255)||'Картинка');
            const uploaded=await api(path('media'),'POST',body);
            if(current!==editing||!select.isConnected||!auth)return;
            media.push(uploaded);
            for(const other of $('fields').querySelectorAll('.image-select'))other.append(new Option(uploaded.originalName+' — '+uploaded.altText,String(uploaded.id)));
            select.value=String(uploaded.id);select.dispatchEvent(new Event('change'));onUploaded();
            status.textContent='Картинка загружена и выбрана. Нажмите «Сохранить», чтобы применить её.';
        }catch(e){if(select.isConnected){status.textContent=e.message;status.classList.add('error');}}
        finally{lock(false);}
    };
}
function required(input,max){input.required=true;if(max)input.maxLength=max;return input;}
async function openEditor(id){const kind=section,ticket=generation;if(catalogs[kind])return openCatalog(kind,id);if(categoryKinds[kind])return openCategory(kind,id);notify('');try{const data=id?await api(path(kind)+'/'+id):{};if(kind!==section||ticket!==generation||!auth)return;current={kind,id,data};$('fields').replaceChildren();$('editError').textContent='';$('editTitle').textContent=(id?'Редактировать: ':'Добавить: ')+sections[kind];$('delete').hidden=!id;$('save').textContent=kind==='media'&&!id?'Загрузить':'Сохранить';
if(kind==='media'){if(!id){const f=required(field('file','Файл (до 15 МБ)','','file'));f.accept='image/jpeg,image/png,image/webp,image/gif,image/avif';}required(field('altText','Описание картинки',data.altText),255);if(id){const img=el('img');img.src=data.url;img.alt=data.altText;img.style.maxWidth='100%';$('fields').append(img);}}
if(kind==='categories'){required(field('name','Название',data.name),80);const slug=required(field('slug','Адрес: латиница, цифры и дефисы',data.slug),80);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';field('sortOrder','Порядок',data.sortOrder||0,'number').min=0;}
if(kind==='leads'){for(const [key,title] of Object.entries({name:'Имя',phone:'Телефон',email:'Почта',message:'Сообщение',contactMethod:'Как связаться',specialist:'Специалист',grades:'Грейды',sourcePage:'Страница заявки',createdAt:'Дата',amoCrmSyncStatus:'Передача в amoCRM',amoCrmLeadId:'Номер сделки',amoCrmSyncError:'Ошибка amoCRM'})){const line=el('p');const value=Array.isArray(data[key])?data[key].join(', '):data[key];line.append(el('strong',title+': '),el('span',value||'—'));$('fields').append(line);}field('processed','Заявка обработана',data.processed,'checkbox');}
if(kind==='articles'){const a=data.article||{};required(field('title','Заголовок',a.title),240);const slug=required(field('slug','Адрес статьи: латиница, цифры и дефисы',a.slug),160);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';field('excerpt','Краткое описание',a.excerpt,'textarea').maxLength=2000;imageField('coverMediaId','Обложка',a.cover?.id);field('status','Публикация',a.status||'DRAFT','select',[['DRAFT','Черновик'],['PUBLISHED','Опубликовать']]);const date=field('publishedAt','Дата публикации (пусто — сейчас или прежняя дата)',a.publishedAt?localDate(a.publishedAt):'','datetime-local');date.step=1;const cat=field('categoryIds','Категории (можно выбрать несколько)','','select',categories.map(c=>[String(c.id),c.name]));cat.multiple=true;for(const c of a.categories||[])if(!categories.some(existing=>existing.id===c.id))cat.append(new Option(c.name,String(c.id)));for(const o of cat.options)o.selected=(a.categories||[]).some(c=>String(c.id)===o.value);field('readingMinutes','Минуты чтения (пусто — автоматически)',id?a.readingMinutes:'','number').min=1;field('seoTitle','Заголовок для поисковиков',data.seoTitle).maxLength=240;field('seoDescription','Описание для поисковиков',data.seoDescription,'textarea').maxLength=500;$('fields').append(el('h3','Содержимое статьи'));const blocks=el('div');blocks.id='blocks';$('fields').append(blocks);for(const b of data.blocks||[])addBlock(b);$('fields').append(button('+ Добавить блок',()=>addBlock({type:'PARAGRAPH'})));}
$('editor').showModal();if(kind==='articles')loadEditorChoices(current);}catch(e){notify(e.message);}}
async function loadEditorChoices(editing){
    const status=el('p','Загружаем список картинок'+(editing.kind==='articles'?' и категорий':'')+'…');
    $('fields').prepend(status);
    const imageSelects=[...$('fields').querySelectorAll('.image-select')];
    const categorySelect=$('fields').querySelector('[name=categoryIds]');
    imageSelects.forEach(n=>n.disabled=true);if(categorySelect)categorySelect.disabled=true;
    const requests=[api(path('media'))];if(categorySelect)requests.push(api(path('categories')));
    const results=await Promise.allSettled(requests);
    if(current!==editing||!$('editor').open||!auth)return;
    const failures=[];
    if(results[0].status==='fulfilled'){
        const selectedIds=[...$('fields').querySelectorAll('.image-select')].map(n=>n.value);
        const uploadedSelections=media.filter(m=>selectedIds.includes(String(m.id))&&!results[0].value.some(v=>v.id===m.id));
        media=[...results[0].value,...uploadedSelections];
        for(const n of $('fields').querySelectorAll('.image-select')){
            const selected=n.value;n.replaceChildren(new Option('Не выбрано',''));
            media.forEach(m=>n.append(new Option(m.originalName+' — '+m.altText,String(m.id))));
            if(selected&&!media.some(m=>String(m.id)===selected))n.append(new Option('Выбранная картинка',selected));
            n.value=selected;n.dispatchEvent(new Event('change'));
        }
    }else failures.push('Картинки: '+results[0].reason.message);
    if(categorySelect){
        if(results[1].status==='fulfilled'){
            const selected=[...categorySelect.selectedOptions].map(o=>o.value);
            const previous=[...categorySelect.options].map(o=>({id:o.value,name:o.text}));
            categories=results[1].value;categorySelect.replaceChildren();
            for(const c of categories)categorySelect.append(new Option(c.name,String(c.id),false,selected.includes(String(c.id))));
            for(const c of previous)if(selected.includes(c.id)&&!categories.some(v=>String(v.id)===c.id))categorySelect.append(new Option(c.name,c.id,false,true));
        }else failures.push('Категории: '+results[1].reason.message);
        categorySelect.disabled=false;
    }
    $('fields').querySelectorAll('.image-select').forEach(n=>n.disabled=false);
    status.replaceChildren();
    if(failures.length){status.append(el('span',failures.join('\n')+' '),button('Повторить загрузку списков',()=>{status.remove();loadEditorChoices(editing);}));}
    else status.remove();
}
function localDate(s){const d=new Date(s);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19);}
function addBlock(data){const block=el('div',undefined,'block'),type=el('select');const types={PARAGRAPH:'Текст',HEADING:'Заголовок',QUOTE:'Цитата',BULLET_LIST:'Список',NUMBERED_LIST:'Нумерованный список',IMAGE:'Картинка'};for(const [v,t] of Object.entries(types))type.append(new Option(t,v));type.value=data.type;type.className='blockType';const controls=el('div',undefined,'toolbar');controls.append(button('↑',()=>{if(block.previousElementSibling)block.parentNode.insertBefore(block,block.previousElementSibling);}),button('↓',()=>{if(block.nextElementSibling)block.parentNode.insertBefore(block.nextElementSibling,block);}),button('Убрать',()=>block.remove()));const fields=el('div');block.append(type,fields,controls);function draw(b={}){fields.replaceChildren();function input(key,title,value,tag='input'){const label=el('label',title),n=el(tag);n.dataset.key=key;n.value=value??'';label.append(n);fields.append(label);return n;}if(type.value==='IMAGE'){const n=input('mediaId','Картинка',b.mediaId,'select');n.append(new Option('Выберите картинку',''));media.forEach(m=>n.append(new Option(m.originalName,String(m.id))));if(b.mediaId&&!media.some(m=>m.id===b.mediaId))n.append(new Option('Выбранная картинка',String(b.mediaId)));n.value=b.mediaId||'';n.required=true;attachImageUpload(n);input('alt','Описание',b.alt);input('caption','Подпись',b.caption);}else if(type.value.includes('LIST')){input('items','Каждый пункт с новой строки',(b.items||[]).join('\n'),'textarea').required=true;}else{const t=input('text','Текст',b.text,'textarea');t.required=true;t.maxLength=20000;if(type.value==='HEADING'){const level=input('level','Уровень заголовка',b.level||2,'select');[2,3,4].forEach(v=>level.append(new Option('H'+v,String(v))));level.value=b.level||2;}}}type.onchange=()=>draw();draw(data);$('blocks').append(block);}
function values(){const out={};for(const n of $('fields').querySelectorAll('[name]')){out[n.name]=n.type==='checkbox'?n.checked:n.multiple?[...n.selectedOptions].map(o=>Number(o.value)):n.value;}return out;}
function blocksValue(){return [...$('blocks').children].map(block=>{const b={type:block.querySelector('.blockType').value};for(const n of block.querySelectorAll('[data-key]')){const k=n.dataset.key;b[k]=k==='items'?n.value.split('\n').map(v=>v.trim()).filter(Boolean):['level','mediaId'].includes(k)?Number(n.value):n.value;}return b;});}
function lock(value){busy=value;for(const b of $('editForm').querySelectorAll('button'))b.disabled=value;}
$('editForm').onsubmit=async e=>{e.preventDefault();if(busy)return;const {kind,id,data}=current,v=values();lock(true);$('editError').textContent='';try{let body=v,method=id?'PUT':'POST';if(kind==='content'){v.mediaId=v.mediaId?Number(v.mediaId):null;v.sortOrder=Number(v.sortOrder);}if(kind==='categories'){v.sortOrder=Number(v.sortOrder);if(id)v.version=data.version;}if(kind==='leads'){body={processed:v.processed};method='PATCH';}if(kind==='media'&&!id){body=new FormData();const file=$('fields').querySelector('[name=file]').files[0];if(file.size>15*1024*1024)throw Error('Файл больше 15 МБ. Выберите меньшую картинку.');body.append('file',file);body.append('altText',v.altText);}if(kind==='articles'){v.coverMediaId=v.coverMediaId?Number(v.coverMediaId):null;v.readingMinutes=v.readingMinutes?Number(v.readingMinutes):null;v.publishedAt=v.publishedAt?new Date(v.publishedAt).toISOString():null;v.blocks=blocksValue();if(id)v.version=data.article.version;if(v.status==='PUBLISHED'&&(!v.excerpt.trim()||!v.coverMediaId||!v.categoryIds.length||!v.blocks.length))throw Error('Для публикации добавьте описание, обложку, категорию и содержимое.');}await api(path(kind)+(id?'/'+id:''),method,body);$('editor').close();await load();notify('Сохранено.',true);}catch(e){$('editError').textContent=e.message;}finally{lock(false);}};
$('delete').onclick=async()=>{if(busy||!confirm('Удалить запись навсегда?'))return;lock(true);try{await api(path(current.kind)+'/'+current.id,'DELETE');$('editor').close();await load();notify('Запись удалена.',true);}catch(e){$('editError').textContent=e.message;}finally{lock(false);}};

function beginEditor(kind,id,data,title,removable=true){
    current={kind,id,data,read:{}};
    $('fields').replaceChildren();$('editError').textContent='';$('editTitle').textContent=title;
    $('delete').hidden=!id||!removable;$('save').textContent='Сохранить';
    return current;
}
function showEditor(){if(!$('editor').open)$('editor').showModal();}
function scalar(parent,title,value='',type='text',options){
    const label=el('label',title),input=el(options?'select':type==='textarea'?'textarea':'input');
    if(options){for(const [key,text] of options)input.append(new Option(text,String(key)));}
    else if(type!=='textarea')input.type=type;
    if(type==='checkbox')input.checked=!!value;else input.value=value??'';
    if(type==='textarea')input.maxLength=50000;
    label.append(input);parent.append(label);
    return input;
}
function pictureInput(parent,title,value){
    const options=[['','Не выбрано'],...media.map(item=>[String(item.id),item.originalName+' — '+item.altText])];
    if(value&&!media.some(item=>String(item.id)===String(value)))options.push([String(value),'Выбранное изображение']);
    const input=scalar(parent,title,value,'select',options);
    attachImageUpload(input);return input;
}
function repeatEditor(parent,title,definitions,initial=[],maximum=100){
    const container=el('section',undefined,'repeat-editor'),heading=el('h3',title),list=el('div');
    container.append(heading,list);parent.append(container);
    function add(data={}){
        if(list.children.length>=maximum)return;
        const row=el('div',undefined,'block'),inputs={};
        list.append(row);
        for(const [key,label,type='text',options] of definitions){
            inputs[key]=type==='image'?pictureInput(row,label,data[key]):scalar(row,label,data[key],type==='requiredTextarea'?'textarea':type,options);
            if(type==='required') {inputs[key].type='text';inputs[key].required=true;inputs[key].maxLength=500;}
            if(type==='requiredTextarea')inputs[key].required=true;
            if(type==='number'){inputs[key].min=1;if(options)inputs[key].required=true;}
        }
        row.read=()=>Object.fromEntries(definitions.map(([key,,type])=>[key,type==='image'||type==='number'?inputs[key].value?Number(inputs[key].value):null:inputs[key].value]));
        const controls=el('div',undefined,'toolbar');
        controls.append(button('↑',()=>{if(row.previousElementSibling)list.insertBefore(row,row.previousElementSibling);}),button('↓',()=>{if(row.nextElementSibling)list.insertBefore(row.nextElementSibling,row);}),button('Убрать',()=>row.remove()));
        row.append(controls);
    }
    initial.forEach(add);container.append(button('+ Добавить',()=>add()));
    return ()=>[...list.children].map(row=>row.read());
}
async function allEntries(kind){
    const items=[];let index=0,result;
    do{result=await api('/api/admin/catalog/'+kind+'?page='+index+'&size=100');items.push(...result.items);index++;}while(index<result.totalPages);
    return items;
}
async function openCategory(kind,id){
    const ticket=generation;
    try{
        const data=id?await api(path(kind)+'/'+id):{};
        if(ticket!==generation||section!==kind||!auth)return;
        beginEditor(kind,id,data,(id?'Редактировать: ':'Добавить: ')+sections[kind]);
        required(field('name','Название',data.name),120);
        const slug=required(field('slug','Адрес категории',data.slug),80);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';
        field('sortOrder','Порядок',data.sortOrder||0,'number').min=0;showEditor();
    }catch(error){notify(error.message);}
}
async function openCatalog(kind,id){
    const ticket=generation;notify('Загружаем форму…');
    try{
        const [data,images,choices,examples]=await Promise.all([id?api(path(kind)+'/'+id):{},api(path('media')),api(path(kind)+'/categories'),kind==='services'?allEntries('cases'):[]]);
        if(ticket!==generation||section!==kind||!auth)return;
        media=images;
        const editing=beginEditor(kind,id,data,(id?'Редактировать: ':'Добавить: ')+sections[kind]);
        const entry=data.entry||{};
        required(field('title',kind==='team'?'Имя':'Название',entry.title),240);
        const slug=required(field('slug','Адрес записи (часть ссылки после последнего /)',entry.slug),160);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';
        const preview=el('p');const prefix={services:'/services/',projects:'/cases/',specialists:'/outstaff/',team:'/team/'}[kind];
        const updateLink=()=>{preview.textContent='Ссылка: '+prefix+slug.value;};slug.oninput=updateLink;updateLink();$('fields').append(preview);
        field('description','Описание',entry.description,'textarea').maxLength=50000;
        if(kind!=='team')field('price','Стоимость',entry.price).maxLength=240;
        imageField('coverMediaId',kind==='team'||kind==='specialists'?'Фотография':'Обложка',entry.cover?.id);
        imageField('mobileCoverMediaId','Изображение для мобильной версии (необязательно)',entry.mobileCover?.id);
        const select=field('categoryIds','Категории','','select',choices.map(category=>[String(category.id),category.name]));select.multiple=true;
        for(const option of select.options)option.selected=(entry.categories||[]).some(category=>String(category.id)===option.value);
        field('published','Показывать на сайте',entry.published??false,'checkbox');
        field('sortOrder','Порядок',entry.sortOrder||0,'number').min=0;
        field('seoTitle','SEO: заголовок',data.seoTitle).maxLength=240;
        field('seoDescription','SEO: описание',data.seoDescription,'textarea').maxLength=1000;
        function detailsFields(body,definitions){
            const inputs={};
            for(const [key,label,type] of definitions)inputs[key]=scalar($('fields'),label,body[key],type);
            return ()=>Object.fromEntries(Object.entries(inputs).map(([key,input])=>[key,input.value]));
        }
        const itemDefinitions=[['title','Название','required'],['description','Описание','textarea']];
        if(kind==='services'){
            const body=data.service||{};
            const headers=detailsFields(body,[['examplesTitle','Заголовок примеров работ'],['faqTitle','Заголовок FAQ'],['stackTitle','Заголовок стека'],['stackDescription','Описание стека','textarea'],['processTitle','Заголовок процессов'],['tasksTitle','Заголовок задач'],['audienceTitle','Заголовок аудитории'],['audienceDescription','Описание аудитории','textarea'],['cooperationTitle','Заголовок моделей сотрудничества']]);
            const caseIds=repeatEditor($('fields'),'Примеры работ',[['caseId','Кейс','number', [['','Выберите кейс'],...examples.map(item=>[item.id,item.title+(item.published?'':' (черновик)')])]]],(data.examples||[]).map(item=>({caseId:item.id})));
            const faq=repeatEditor($('fields'),'FAQ',[['question','Вопрос','required'],['answer','Ответ','requiredTextarea']],body.faq||[]);
            const stack=repeatEditor($('fields'),'Стек',[['name','Технология','required'],['mediaId','Логотип','image']],body.stack||[]);
            const process=repeatEditor($('fields'),'Процессы',itemDefinitions,body.process||[]);
            const tasks=repeatEditor($('fields'),'Решаемые задачи',itemDefinitions,body.tasks||[]);
            const audience=repeatEditor($('fields'),'Кому подходит услуга',itemDefinitions,body.audience||[]);
            const cooperation=repeatEditor($('fields'),'Модели сотрудничества',[...itemDefinitions,['price','Стоимость']],body.cooperation||[]);
            editing.read.body=()=>({service:{...headers(),faq:faq(),stack:stack(),process:process(),tasks:tasks(),audience:audience(),cooperation:cooperation()},caseIds:caseIds().map(item=>item.caseId)});
        }else if(kind==='projects'){
            const body=data.project||{};
            const headers=detailsFields(body,[['headline','Заголовок детальной страницы'],['client','Клиент'],['challenge','Задача','textarea'],['solution','Решение','textarea'],['result','Результат','textarea']]);
            const chapters=repeatEditor($('fields'),'Содержимое кейса',itemDefinitions,body.sections||[]);
            const stack=repeatEditor($('fields'),'Стек',[['name','Технология','required'],['mediaId','Логотип','image']],body.stack||[]);
            const gallery=repeatEditor($('fields'),'Галерея',[['mediaId','Изображение','image'],['sourceUrl','Адрес исходного изображения (если файл ещё не загружен)'],['alt','Описание изображения'],['caption','Подпись']],body.gallery||[]);
            editing.read.body=()=>({project:{...headers(),sections:chapters(),stack:stack(),gallery:gallery()}});
        }else if(kind==='specialists'){
            const body=data.specialist||{};
            const headers=detailsFields(body,[['direction','Направление'],['experience','Опыт'],['availability','Доступность']]);
            const grades=scalar($('fields'),'Грейды: каждый с новой строки',(body.grades||[]).join('\n'),'textarea');
            const skills=scalar($('fields'),'Навыки: каждый с новой строки',(body.skills||[]).join('\n'),'textarea');
            editing.read.body=()=>({specialist:{...headers(),grades:grades.value.split('\n').map(value=>value.trim()).filter(Boolean),skills:skills.value.split('\n').map(value=>value.trim()).filter(Boolean)}});
        }else{
            const headers=detailsFields(data.member||{},[['position','Должность'],['biography','Биография','textarea']]);
            editing.read.body=()=>({member:headers()});
        }
        notify('');showEditor();
    }catch(error){notify(error.message);}
}
async function openPage(slug){
    const ticket=generation;
    try{
        const data=await api('/api/admin/pages/'+slug);
        if(ticket!==generation||!auth)return;
        beginEditor('pages',slug,data,data.page.title,false);
        required(field('title','Название страницы',data.page.title),240);
        field('seoTitle','SEO: заголовок',data.seoTitle).maxLength=240;
        field('seoDescription','SEO: описание',data.seoDescription,'textarea').maxLength=1000;
        field('canonical','Канонический адрес',data.canonical).maxLength=1000;
        field('published','Показывать страницу',data.page.published,'checkbox');
        $('fields').append(el('h3','Блоки страницы'),el('p','Структура фиксирована. Откройте блок, чтобы изменить его содержимое. Общие блоки меняются на всех страницах.'));
        for(const block of data.blocks){
            const card=el('div',undefined,'card');card.append(el('span',block.title+(block.ownerPage!==slug?' · общий':'')+(block.published?'':' · скрыт')),button('Открыть блок',()=>openPageBlock(slug,block)));$('fields').append(card);
        }
        if(data.collections.length)$('fields').append(el('h3','Связанные записи'));
        for(const collection of data.collections){
            const card=el('div',undefined,'card');card.append(el('span',collection.key),button('Выбрать записи',()=>openCollection(slug,collection)));$('fields').append(card);
        }
        showEditor();
    }catch(error){notify(error.message);}
}
function fixedValues(parent,schema,values){
    const inputs={};
    for(const [key,rule] of Object.entries(schema)){
        const value=values[key]||{};
        const text=scalar(parent,rule.label,value.text,'textarea');
        if(rule.type==='LINK')inputs[key]={text,href:scalar(parent,'Адрес ссылки',value.href)};
        else if(rule.type==='IMAGE')inputs[key]={text,mediaId:pictureInput(parent,'Изображение',value.mediaId)};
        else inputs[key]={text};
        inputs[key].published=scalar(parent,'Показывать поле',value.published??true,'checkbox');
    }
    return ()=>Object.fromEntries(Object.entries(inputs).map(([key,input])=>[key,{text:input.text.value,href:input.href?.value||null,mediaId:input.mediaId?.value?Number(input.mediaId.value):null,published:input.published.checked}]));
}
async function openPageBlock(returnSlug,block){
    const ticket=generation;
    try{
        const images=await api(path('media'));
        if(ticket!==generation||!auth)return;
        media=images;const owner=block.ownerPage;
        const fresh=await api('/api/admin/pages/'+owner);
        if(ticket!==generation||!auth)return;
        block=fresh.blocks.find(item=>item.id===block.id);if(!block)throw Error('Блок не найден');
        const editing=beginEditor('pageBlock',block.id,block,block.title,false);editing.owner=owner;editing.returnSlug=returnSlug;
        $('fields').append(button('← К странице',()=>openPage(returnSlug)));
        field('published','Показывать блок',block.published,'checkbox');
        const fields=fixedValues($('fields'),block.schema.fields,block.data.fields);
        const list=el('div');$('fields').append(list);
        function add(values={}){
            if(list.children.length>=200)return;
            const row=el('div',undefined,'block');list.append(row);row.read=fixedValues(row,block.schema.itemFields,values);
            const controls=el('div',undefined,'toolbar');controls.append(button('↑',()=>{if(row.previousElementSibling)list.insertBefore(row,row.previousElementSibling);}),button('↓',()=>{if(row.nextElementSibling)list.insertBefore(row.nextElementSibling,row);}),button('Убрать',()=>row.remove()));row.append(controls);
        }
        block.data.items.forEach(add);
        if(Object.keys(block.schema.itemFields).length)$('fields').append(button('+ Добавить элемент списка',()=>add()));
        editing.read.body=()=>({fields:fields(),items:[...list.children].map(row=>row.read())});showEditor();
    }catch(error){$('editError').textContent=error.message;}
}
async function openCollection(slug,collection){
    const ticket=generation;
    try{
        const choices=await allEntries(collection.kind.toLowerCase());
        if(ticket!==generation||!auth)return;
        const editing=beginEditor('pageCollection',collection.id,collection,'Записи: '+collection.key,false);editing.owner=slug;editing.returnSlug=slug;
        $('fields').append(button('← К странице',()=>openPage(slug)));
        field('allEntries','Показывать весь каталог',collection.allEntries,'checkbox');
        $('fields').append(el('p','Переопределения меняют карточку только на этой странице. Пустое поле использует исходное значение записи.'));
        const definitions=[['entryId','Запись','number',[['','Выберите запись'],...choices.map(item=>[item.id,item.title+(item.published?'':' (черновик)')])]],['title','Заголовок на этой странице'],['description','Описание на этой странице','textarea'],['imageAlt','Описание картинки на этой странице']];
        if(collection.kind==='TEAM')definitions.push(['position','Должность на этой странице']);
        if(collection.kind==='SPECIALISTS')definitions.push(['specialistDirection','Направление на этой странице'],['specialistGrades','Грейды на этой странице (каждый с новой строки)','textarea'],['specialistSkills','Навыки на этой странице (каждый с новой строки)','textarea'],['specialistExperience','Опыт на этой странице'],['specialistAvailability','Доступность на этой странице']);
        const selected=repeatEditor($('fields'),'Выбранные записи',definitions,collection.entryIds.map(id=>{
            const override=(collection.overrides||[]).find(item=>item.entryId===id)||{};
            const specialist=override.specialist||{};
            return {entryId:id,...override,specialistDirection:specialist.direction,specialistGrades:(specialist.grades||[]).join('\n'),specialistSkills:(specialist.skills||[]).join('\n'),specialistExperience:specialist.experience,specialistAvailability:specialist.availability};
        }));
        editing.read.body=()=>{
            const rows=selected();return {entryIds:rows.map(item=>item.entryId),overrides:rows.map(item=>{
                const hasSpecialist=item.specialistDirection||item.specialistGrades||item.specialistSkills||item.specialistExperience||item.specialistAvailability;
                const lines=value=>(value||'').split('\n').map(value=>value.trim()).filter(Boolean);
                return {entryId:item.entryId,title:item.title||null,description:item.description||null,position:item.position||null,imageAlt:item.imageAlt||null,specialist:hasSpecialist?{direction:item.specialistDirection||null,grades:lines(item.specialistGrades),skills:lines(item.specialistSkills),experience:item.specialistExperience||null,availability:item.specialistAvailability||null}:null};
            }).filter(item=>item.title||item.description||item.position||item.imageAlt||item.specialist)};
        };showEditor();
    }catch(error){$('editError').textContent=error.message;}
}
const originalSubmit=$('editForm').onsubmit;
$('editForm').onsubmit=async event=>{
    const editing=current;
    if(!editing||(!catalogs[editing.kind]&&!categoryKinds[editing.kind]&&!['pages','pageBlock','pageCollection'].includes(editing.kind)))return originalSubmit(event);
    event.preventDefault();if(busy)return;
    const {kind,id,data}=editing,form=values();let url,body,method=id?'PUT':'POST';
    lock(true);$('editError').textContent='';
    try{
        if(catalogs[kind]){
            body={title:form.title,slug:form.slug,description:form.description,price:form.price||null,seoTitle:form.seoTitle,seoDescription:form.seoDescription,published:form.published,sortOrder:Number(form.sortOrder),coverMediaId:form.coverMediaId?Number(form.coverMediaId):null,categoryIds:form.categoryIds,caseIds:[],...editing.read.body()};
            body.mobileCoverMediaId=form.mobileCoverMediaId?Number(form.mobileCoverMediaId):null;
            if(id)body.version=data.entry.version;
            url=path(kind)+(id?'/'+id:'');
        }else if(categoryKinds[kind]){
            body={name:form.name,slug:form.slug,sortOrder:Number(form.sortOrder)};if(id)body.version=data.version;url=path(kind)+(id?'/'+id:'');
        }else if(kind==='pages'){
            body={...form,version:data.page.version};url='/api/admin/pages/'+id;
        }else if(kind==='pageBlock'){
            body={data:editing.read.body(),published:form.published,version:data.version};url='/api/admin/pages/'+editing.owner+'/blocks/'+id;
        }else{
            body={allEntries:form.allEntries,...(form.allEntries?{entryIds:[],overrides:[]}:editing.read.body()),version:data.version};url='/api/admin/pages/'+editing.owner+'/collections/'+id;
        }
        await api(url,method,body);
        if(editing.returnSlug){lock(false);await openPage(editing.returnSlug);notify('Сохранено.',true);}
        else{$('editor').close();await load();notify('Сохранено.',true);}
    }catch(error){$('editError').textContent=error.message;}finally{lock(false);}
};
